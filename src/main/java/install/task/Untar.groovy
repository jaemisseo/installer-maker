package install.task

import install.configuration.annotation.type.TerminalValueProtocol
import install.configuration.annotation.type.Task
import install.configuration.annotation.Value
import install.util.TaskUtil
import jaemisseo.man.FileMan
import jaemisseo.man.util.FileSetup

/**
 * Created by sujkim on 2017-02-22.
 */
@Task
@TerminalValueProtocol(['from', 'to'])
class Untar extends TaskUtil{

    @Value(property='from', method='getFilePath')
    String filePath

    @Value(property='to', method='getFilePath')
    String destPath

    @Value(method='genMergedFileSetup')
    FileSetup fileSetup



    @Override
    Integer run(){
        println "<UNTAR>"
        FileMan.untar(filePath, destPath, fileSetup)

        return STATUS_TASK_DONE
    }

}
