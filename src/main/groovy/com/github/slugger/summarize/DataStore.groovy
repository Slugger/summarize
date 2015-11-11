/*
 Copyright 2015 Battams, Derek
 
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
 
		http://www.apache.org/licenses/LICENSE-2.0
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package com.github.slugger.summarize

import groovy.sql.Sql
import groovy.util.logging.Log4j

import java.sql.DriverManager
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException

import com.github.slugger.summarize.data.Build
import com.github.slugger.summarize.data.Comment
import com.github.slugger.summarize.data.CompletedTask
import com.github.slugger.summarize.data.LinkedTask
import com.github.slugger.summarize.data.Product
import com.github.slugger.summarize.data.Task

@Log4j
class DataStore {
	static private final String JDBC_DRIVER = 'org.apache.derby.jdbc.EmbeddedDriver'
	
	static private DataStore INSTANCE = null
	synchronized static DataStore getInstance() {
		if(!INSTANCE)
			INSTANCE = new DataStore()
		return INSTANCE
	}

	private final Sql sql = null
	
	private DataStore() {
		def jdbcStr = 'jdbc:derby:/var/lib/summarize/db' //;create=true'
		log.info "Connecting to database: $jdbcStr"
		try {
			sql = Sql.newInstance(jdbcStr, JDBC_DRIVER)
			log.info 'Connected to existing database'
		} catch(SQLException e) {
			if(e.SQLState == 'XJ004') {
				jdbcStr += ';create=true'
				sql = Sql.newInstance(jdbcStr, JDBC_DRIVER)
				createTables()
				setDbVersion()
				log.info 'New database created'
			} else
				throw e
		}
	}

	void shutdown() {
		try {
			sql.close()
			DriverManager.getConnection("jdbc:derby:/var/lib/summarize/db;shutdown=true");
		} catch(SQLException e) {
			if(e.SQLState != '08006')
				throw e
			else
				log.info 'Database shutdown successfully!'
		}
	}
	
	LinkedTask[] getLinkedTasksForProduct(Product p) {
		def tasks = []
		sql.eachRow("SELECT * FROM does_task WHERE prod_id = $p.id") {
			tasks << new LinkedTask(it)
		}
		tasks
	}
	
	CompletedTask updateBuild(CompletedTask ct)	{
		def now = new Date()
		def taskLinkId = ct.linkedTask.id
		sql.withTransaction {
			sql.execute("DELETE FROM completed_task WHERE build_id = ${ct.build.id} AND does_task_id = $taskLinkId")
			sql.execute("INSERT INTO completed_task (build_id, does_task_id, start, finish, updated, updated_by, url, brief_desc, state) VALUES ($ct.build.id, $taskLinkId, ${ct.start ?: now}, $ct.finish, $now, ${ct.updatedBy ?: 'automation'}, $ct.url, $ct.description, $ct.state)")
			sql.execute("UPDATE does_task SET ordering = $ct.linkedTask.order WHERE id = $taskLinkId")
		}
		ct
	}
	
	LinkedTask updateLinkedTask(LinkedTask lt) {
		sql.execute("UPDATE does_task SET ordering = $lt.order WHERE id = $lt.id")
		lt
	}
	
	Build registerBuild(long prodId, String build) {
		def row = sql.firstRow("SELECT id FROM prod_build WHERE prod_id = $prodId AND build = $build") 
		if(!row)
			getBuildById((sql.executeInsert("INSERT INTO prod_build (prod_id, build) VALUES ($prodId, $build)")[0][0]).toLong())
		else
			getBuildById(row.id)
	}
	
	Product getProduct(String name, String version) {
		def row = sql.firstRow("SELECT * FROM product WHERE name = $name AND version = $version")
		row ? new Product(row) : null
	}
	
	Task getTaskByName(String name) {
		def row = sql.firstRow("SELECT * FROM task WHERE name = $name")
		row ? new Task(row) : null
	}
	
	Product addProduct(String name, String version, String desc) {
		getProductById((sql.executeInsert("INSERT INTO product (name, version, description) VALUES ($name, $version, $desc)")[0][0]).toLong())
	}
	
	Task addTask(String name, String desc) {
		getTaskById((sql.executeInsert("INSERT INTO task (name, description) VALUES ($name, $desc)")[0][0]).toLong())
	}
	
	Product[] getProducts() {
		def map = []
		sql.eachRow("SELECT * FROM product") {
			map << new Product(it)
		}
		map as Product[]
	}
	
	Task[] getTasks() {
		def map = []
		sql.eachRow("SELECT * FROM task") {
			map << new Task(it)
		}
		map as Task[]
	}
		
	Task[] getTasks(Product p) {
		def qry = "SELECT id FROM tasks_for_prod WHERE prod_name = $p.name AND prod_ver = $p.version"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(qry)
			def qryStr = sql.asSql(qry, params)
			log.trace "$qryStr $params"
		}
		def tasks = []
		sql.eachRow(qry) {
			def row = sql.firstRow("SELECT * FROM task WHERE id = ${it[0]}")
			if(row)
				tasks << new Task(row)
		}
		tasks
	}
	
	Build getBuildById(long id) {
		def qry = "SELECT * FROM prod_build WHERE id = $id"
		log.trace qry
		def row = sql.firstRow(qry)
		row ? new Build(row) : null
	}

	Task getTaskById(long id) {
		def qry = "SELECT * FROM task WHERE id = $id"
		log.trace qry
		def row = sql.firstRow(qry)
		row ? new Task(row) : null
	}

	Map<Build, List<CompletedTask>> getBuildsForProduct(Product p) {
		def qry = "SELECT * FROM tasks_completed WHERE p_id = $p.id"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(qry)
			def qryStr = sql.asSql(qry, params)
			log.trace "$qryStr $params"
		}
		def builds = [:]
		sql.eachRow(qry) {
			def bld = getBuildById(it.pb_id)
			def tasks = builds[bld]
			if(tasks == null) {
				tasks = []
				builds[bld] = tasks
			}
			def ct = getCompletedTaskById(it.ct_id)
			if(ct)
				tasks << ct
		}
		builds.sort { -1L * it.value.min { it.start }.start.time }
	}
	
	CompletedTask getCompletedTaskById(long id) {
		def qry = "SELECT * FROM completed_task WHERE id = $id"
		log.trace qry
		def row = sql.firstRow(qry)
		row ? new CompletedTask(row) : null
	}
	
	String getSetting(String name, String defaultValue = null) {
		def qry = "SELECT value FROM settings WHERE name = $name"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(qry)
			def qryStr = sql.asSql(qry, params)
			log.trace "$qryStr $params"
		}
		return sql.firstRow(qry)?.value ?: defaultValue
	}

	void setSetting(String name, String value) {
		def delQry = "DELETE FROM settings WHERE name = $name"
		def insQry = "INSERT INTO settings (name, value) VALUES ($name, $value)"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(delQry)
			def qryStr = sql.asSql(delQry, params)
			log.trace "$qryStr $params"
			params = sql.getParameters(insQry)
			qryStr = sql.asSql(insQry, params)
			log.trace "$qryStr $params"
		}
		sql.withTransaction {
			sql.execute delQry
			if(sql.executeUpdate(insQry) != 1)
				throw new RuntimeException('DBError writing setting value')
		}
	}
	
	void link(long prodId, long[] taskIds) {
		def delQry = "DELETE FROM does_task WHERE prod_id = $prodId AND task_id NOT IN (${taskIds.toList().join(',')})".toString()
		log.trace delQry
		sql.withTransaction {
			sql.execute delQry
			taskIds.each {
				def insQry = "INSERT INTO does_task (prod_id, task_id, ordering) VALUES ($prodId, $it, 1)"
				if(log.isTraceEnabled()) {
					def params = sql.getParameters(insQry)
					def qryStr = sql.asSql(insQry, params)
					log.trace "$qryStr $params"
				}
				try {
					sql.execute insQry
				} catch(SQLIntegrityConstraintViolationException e) {
					log.warn "Ignoring failure to link prodId $prodId -> taskId $it; assuming they're already linked!"
				}
			}
		}
	}
	
	Product getProductById(long id) {
		def qry = "SELECT * FROM product WHERE id = $id"
		log.trace qry
		def row = sql.firstRow(qry)
		row ? new Product(row) : null
	}
	
	Comment[] getComments(CompletedTask ct) {
		def comments = []
		def qry = "SELECT * FROM notes WHERE c_task_id = $ct.id ORDER BY updated DESC"
		log.trace qry
		sql.eachRow(qry) {
			comments << new Comment(it)
		}
		comments
	}
	
	void addComment(long completedTaskId, String author, String comment, String updatedBy = null) {
		def now = new Date()
		def qry = "INSERT INTO notes (c_task_id, author, comment, created, updated, updated_by) VALUES ($completedTaskId, $author, $comment, $now, $now, ${updatedBy ?: (author ?: 'automation')})"
		log.trace qry
		sql.execute qry
	}
		
	LinkedTask getLinkedTaskById(long id) {
		def qry = "SELECT * FROM does_task WHERE id = $id"
		log.trace qry
		def row = sql.firstRow(qry)
		row ? new LinkedTask(row) : null
	}
	
	private void setDbVersion() {
		def qry = "INSERT INTO settings (name, value) VALUES ('dbVersion', '0')"
		if(log.isTraceEnabled())
			log.trace qry
		sql.execute qry
	}
	
	private void createTables() {
		sql.withTransaction {
			sql.execute '''
				CREATE TABLE product (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					name VARCHAR(128) NOT NULL,
					version VARCHAR(128) NOT NULL,
					description VARCHAR(255),
					CONSTRAINT unique_name_ver UNIQUE (name, version)
				)
			'''
			
			sql.execute '''
				CREATE TABLE prod_build (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					prod_id BIGINT NOT NULL,
					build VARCHAR(64) NOT NULL,
					CONSTRAINT prod_build_prod_id_ref FOREIGN KEY (prod_id) REFERENCES product(id) ON DELETE CASCADE,
					CONSTRAINT unique_prod_bld UNIQUE (prod_id, build)
				)
			'''
			
			sql.execute '''
				CREATE TABLE task (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					name VARCHAR(64) NOT NULL CONSTRAINT unique_name UNIQUE,
					description VARCHAR(255)
				)
			'''
			
			sql.execute '''
				CREATE TABLE does_task (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					task_id BIGINT NOT NULL,
					prod_id BIGINT NOT NULL,
					ordering INT NOT NULL,
					CONSTRAINT does_task_task_id_ref FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
					CONSTRAINT does_task_prod_id_ref FOREIGN KEY (prod_id) REFERENCES product(id) ON DELETE CASCADE,
					CONSTRAINT unique_task_prod UNIQUE (task_id, prod_id)
				)
			'''
			
			sql.execute '''
				CREATE TABLE completed_task (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					does_task_id BIGINT NOT NULL,
					build_id BIGINT NOT NULL,
					start TIMESTAMP NOT NULL,
					finish TIMESTAMP,
					updated TIMESTAMP NOT NULL,
					updated_by VARCHAR(128) NOT NULL,
					state CHAR(4),
					url LONG VARCHAR,
					brief_desc VARCHAR(32) NOT NULL,
					CONSTRAINT completed_task_does_task_id_ref FOREIGN KEY (does_task_id) REFERENCES does_task(id) ON DELETE CASCADE,
					CONSTRAINT completed_task_build_id_ref FOREIGN KEY (build_id) REFERENCES prod_build(id) ON DELETE CASCADE,
					CONSTRAINT unique_does_bld UNIQUE (does_task_id, build_id),
					CONSTRAINT valid_states CHECK (state IN ('RUN', 'PASS', 'FAIL', 'WARN'))
				)
			'''
						
			sql.execute '''
				CREATE TABLE notes (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					c_task_id BIGINT NOT NULL,
					author VARCHAR(128) NOT NULL,
					comment LONG VARCHAR NOT NULL,
					created TIMESTAMP NOT NULL,
					updated TIMESTAMP NOT NULL,
					updated_by VARCHAR(128) NOT NULL,
					CONSTRAINT notes_c_task_id_ref FOREIGN KEY (c_task_id) REFERENCES completed_task(id) ON DELETE CASCADE
				)
			'''
			
			sql.execute '''
				CREATE TABLE settings (
					name VARCHAR(64) NOT NULL PRIMARY KEY,
					value VARCHAR(128)
				)
			'''
			
			sql.execute '''
				CREATE VIEW tasks_completed AS
					SELECT ct.id AS ct_id, t.id AS t_id, p.id AS p_id, pb.id AS pb_id, dt.id AS dt_id FROM
							app.product AS p LEFT OUTER JOIN app.prod_build AS pb ON (p.id = pb.prod_id)
							LEFT OUTER JOIN app.completed_task AS ct ON (ct.build_id = pb.id)
							LEFT OUTER JOIN app.does_task AS dt ON (dt.prod_id = p.id AND dt.id = ct.does_task_id)
							LEFT OUTER JOIN app.task AS t ON (t.id = dt.task_id)
						WHERE ct.id IS NOT NULL
			'''
			
			sql.execute '''
				CREATE VIEW tasks_for_prod AS
					SELECT t.id AS id, t.name AS name, t.description AS task_desc, p.name AS prod_name,
						p.version AS prod_ver, p.description AS prod_desc FROM
							app.task AS t LEFT OUTER JOIN app.does_task AS dt ON (t.id = dt.task_id)
							LEFT OUTER JOIN app.product AS p ON (dt.prod_id = p.id)
						WHERE p.name IS NOT NULL ORDER BY ordering ASC
			'''
		}
	}
}
