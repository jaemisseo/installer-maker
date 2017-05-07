package install.task

import jaemisseo.man.FileMan
import jaemisseo.man.util.FileSetup

/**
 * Created by sujkim on 2017-02-22.
 */
class TaskFileUntar extends TaskUtil{

    @Override
    Integer run(){

        //Ready
        String filePath = getFilePath('file.path')
        String destPath = getFilePath('dest.path')
        FileSetup fileSetup = genMergedFileSetup()

        //DO
        println "<UNTAR>"
        FileMan.untar(filePath, destPath, fileSetup)

        return STATUS_TASK_DONE
    }

}
