package install.task

import jaemisseo.man.configuration.annotation.Alias
import jaemisseo.man.configuration.annotation.HelpIgnore
import jaemisseo.man.configuration.annotation.Value
import jaemisseo.man.configuration.annotation.type.Task
import install.util.TaskUtil

/**
 * Created by sujkim on 2017-02-22.
 */
@Alias('v')
@Task
class Version extends TaskUtil{

    @HelpIgnore
    @Value('application.name')
    String applicationName = 'installer-maker'

    @Override
    Integer run(){

        provider.printVersion()

        java.lang.System.exit(0)

        return STATUS_TASK_DONE
    }

}
