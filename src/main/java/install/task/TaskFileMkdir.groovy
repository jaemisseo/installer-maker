package install.task

import com.jaemisseo.man.FileMan
import com.jaemisseo.man.PropMan
import com.jaemisseo.man.util.FileSetup

/**
 * Created by sujkim on 2017-02-22.
 */
class TaskFileMkdir extends TaskUtil{

    @Override
    void run(){

        //Ready
        String destPath = getFilePath('dest.path')
        Map buildStructureMap = getMap('structure')
        FileSetup fileSetup = genMergedFileSetup()

        //DO
        println "<MKDIR>"
        FileMan.mkdirs(destPath, buildStructureMap)

    }

}
