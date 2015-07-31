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

import org.apache.log4j.FileAppender
import org.apache.log4j.Layout

/**
 * Custom file appender that keeps old files around
 * @author ddb
 *
 */
class ArchivingFileAppender extends FileAppender {


	ArchivingFileAppender() { super() }
	
	ArchivingFileAppender(Layout layout, String filename) throws IOException {
		super(layout, filename);
	}

	ArchivingFileAppender(Layout layout, String filename, boolean append)
			throws IOException {
		super(layout, filename, append);
	}

	ArchivingFileAppender(Layout layout, String filename, boolean append,
			boolean bufferedIO, int bufferSize) throws IOException {
		super(layout, filename, append, bufferedIO, bufferSize);
	}

	@Override
	void activateOptions() {
		if(fileName) {
			def f = new File(fileName).absoluteFile
			archive(f)
			if(f.exists())
				f.renameTo(new File(f.parentFile, "${f.name}.1"))
			setFile(f.absolutePath, fileAppend, bufferedIO, bufferSize)
		}
	}
	
	protected void archive(File base) {
		def i = 0
		def f
		while((f = new File(base.parentFile, "${base.name}.${++i}")).exists());
		for(int j = i - 1; j > 0; --j) {
			f = new File(base.parentFile, "${base.name}.$j")
			f.renameTo(new File(f.parentFile, "${base.name}.${j + 1}"))
		}
	}
}