package install.job

import install.bean.GlobalOptionForInstallerMaker
import install.bean.ReportSetup
import install.bean.TaskSetup
import install.exception.WantToRestartException
import jaemisseo.man.configuration.Config
import jaemisseo.man.configuration.annotation.HelpIgnore
import jaemisseo.man.configuration.annotation.method.Command
import jaemisseo.man.configuration.annotation.method.Init
import jaemisseo.man.configuration.annotation.type.Document
import jaemisseo.man.configuration.annotation.type.Job
import jaemisseo.man.configuration.annotation.type.Task
import jaemisseo.man.configuration.data.PropertyProvider
import install.util.JobUtil
import jaemisseo.man.FileMan
import jaemisseo.man.PropMan
import jaemisseo.man.ReportMan
import install.bean.FileSetup
import jaemisseo.man.util.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by sujkim on 2017-02-17.
 */
@Job
class InstallerMaker extends JobUtil{

    final Logger logger = LoggerFactory.getLogger(this.getClass());
    int jobCallingCount = 0

    InstallerMaker(){
        propertiesFileName = 'installer-maker'
        jobName = 'installer-maker'
    }

    void logo(){
        logger.info Util.multiTrim("""
        88                                          88 88                                       
        ''                         ,d               88 88                                       
                                   88               88 88                                       
        88 8b,dPPYba,  ,adPPYba, MM88MMM ,adPPYYba, 88 88  ,adPPYba, 8b,dPPYba,                 
        88 88P'   ''8a I8[    ''   88    ''     'Y8 88 88 a8P_____88 88P'   'Y8                 
        88 88       88  ''Y8ba,    88    ,adPPPPP88 88 88 8PP''''''' 88                         
        88 88       88 aa    ]8I   88,   88,    ,88 88 88 '8b,   ,aa 88                         
        88 88       88 ''YbbdP''   'Y888 ''8bbdP'Y8 88 88  ''Ybbd8'' 88                         
                                                                                                
                                      88                                                        
                                      88                                                        
                                      88                                                        
        88,dPYba,,adPYba,  ,adPPYYba, 88   ,d8  ,adPPYba, 8b,dPPYba,                            
        88P'   '88'    '8a ''     'Y8 88 ,a8'  a8P_____88 88P'   'Y8                            
        88      88      88 ,adPPPPP88 8888[    8PP''''''' 88                                    
        88      88      88 88,    ,88 88''Yba, '8b,   ,aa 88                                    
        88      88      88 ''8bbdP'Y8 88   'Y8a ''Ybbd8'' 88                                    
        """)
    }

    @Init(lately=true)
    void init(){
        //Parse Global Property's variable
        this.propman = setupPropMan(provider)
        this.varman = setupVariableMan(propman)

        //Inject default value to GlobalOption
        provider.shift(jobName)
        this.gOpt = config.injectValue(new GlobalOptionForInstallerMaker())

        //Make Virtual Command
        this.virtualPropman = new PropMan()
        cacheAllCommitTaskListOnAllCommand()

        //First Commit
        commit()
    }

    PropMan setupPropMan(PropertyProvider provider){
        PropMan propmanForInstallerMaker = provider.propGen.get('installer-maker')
        PropMan propmanDefault = provider.propGen.getDefaultProperties()
        PropMan propmanProgram = provider.propGen.getProgramProperties()
        PropMan propmanExternal = provider.propGen.getExternalProperties()

        //- Try to get from User's FileSystem
        String propertiesDir = propmanExternal.get('properties.dir') ?: propmanDefault.get('user.dir')
        if (propertiesDir)
            propertiesFile = FileMan.find(propertiesDir, propertiesFileName, ["yml", "yaml", "properties"])

        //- Make Property Manager
        if (propertiesFile && propertiesFile.exists()){
            propertiesFileExtension = FileMan.getExtension(propertiesFile)
            Map propertiesMap = generateMapFromPropertiesFile(propertiesFile)
            propmanForInstallerMaker.merge(propertiesMap)
                            .merge(propmanExternal)
                            .mergeNew(propmanDefault)
                            .mergeNew(propmanProgram)
                            .merge(['builder.home': FileMan.getFullPath(propmanDefault.get('lib.dir'), '../')])
        }

        return propmanForInstallerMaker
    }



    @Command
    void customCommand(){
        //Setup Log
        setupLog(gOpt.logSetup)

        if (!jobCallingCount++)
            logo()

        if (!propertiesFile)
            throw new Exception('Does not exists script file [ installer-maker.yml ]')

        ReportSetup reportSetup = config.injectValue(new ReportSetup())

        //Each level by level
        validTaskList = Config.findAllClasses('install', [Task])
        eachTaskWithCommit(commandName){ TaskSetup commitTask ->
            try{
                return runTaskByCommitTask(commitTask)
            }catch(WantToRestartException wtre){
                throw wtre
            }catch(Exception e){
                //Write Report
                writeReport(reportMapList, reportSetup)
                throw e
            }
        }

        //Write Report
        writeReport(reportMapList, reportSetup)
    }



    @Command('init')
    @Document("""
    Init 2 Installer-Maker script files
    You can generate Sample Properties Files to build installer (installer-maker.yml, installer.yml)
    
        installer-maker init    
    
    You can generate Default Properties Files (installer-maker.default.properties, installer.default.properties) 
            
        installer-maker init --default
    
    And you can create custum task manager script (hoya.yml) 
        
        hoya init
            
    Default.. (hoya.default.properties)
    
        hoya init --default
    """)
    void initCommand(){
        //Ready
        FileSetup fileSetup = gOpt.fileSetup
        fileSetup.modeAutoOverWrite = false
        String propertiesDir = provider.getFilePath('properties.dir') ?: FileMan.getFullPath('./')
        String fileFrom
        String fileTo

        //DO
        logger.info "<Init File>"
        logger.info "- Dest Path: ${propertiesDir}"

        PropMan externalPropMan = config.propGen.getExternalProperties()
        List<String> dashDashOptionList = externalPropMan.get('--')

        /** Default Properties **/
        if (dashDashOptionList.contains('default')){
            if (dashDashOptionList.contains('hoya')) {
                try{
                    fileFrom = "defaultProperties/hoya.default.properties"
                    fileTo = "${propertiesDir}/hoya.default.properties"
                    new FileMan().readResource(fileFrom).write(fileTo, fileSetup)
                }catch(e){
                    logger.warn "File Aready Exists. ${fileTo}\n"
                }
            }else{
                try{
                    fileFrom = "defaultProperties/installer-maker.default.properties"
                    fileTo = "${propertiesDir}/installer-maker.default.properties"
                    new FileMan().readResource(fileFrom).write(fileTo, fileSetup)
                }catch(e){
                    logger.warn "File Aready Exists. ${fileTo}\n"
                }

                try{
                    fileFrom = "defaultProperties/installer.default.properties"
                    fileTo = "${propertiesDir}/installer.default.properties"
                    new FileMan().readResource(fileFrom).write(fileTo, fileSetup)
                }catch(e){
                    logger.warn "File Aready Exists. ${fileTo}\n"
                }
            }

        /** Properties **/
        }else{
            if (dashDashOptionList.contains('hoya')){
                try{
                    fileFrom = "hoya.yml"
                    fileTo = "${propertiesDir}/hoya.yml"
                    new FileMan().readResource(fileFrom).write(fileTo, fileSetup)
                }catch(e){
                    logger.warn "File Aready Exists. ${fileTo}\n"
                }

            }else{
                try{
                    fileFrom = "sampleProperties/installer-maker.sample.yml"
                    fileTo = "${propertiesDir}/installer-maker.yml"
                    new FileMan().readResource(fileFrom).write(fileTo, fileSetup)
                }catch(e){
                    logger.warn "File Aready Exists. ${fileTo}\n"
                }

                try{
                    fileFrom = "sampleProperties/installer.sample.yml"
                    fileTo = "${propertiesDir}/installer.yml"
                    new FileMan().readResource(fileFrom).write(fileTo, fileSetup)
                }catch(e){
                    logger.warn "File Aready Exists. ${fileTo}\n"
                }
            }
        }
    }

    @Command('clean')
    @Document('''
    Clean Build Directory
    
        You need installer-maker Script file(installer-maker.yml) 
    
    - Options
        1. You can change your build directory on Builder Script  
            [default value list]
                installer.name: installer_myproject                        
                build.dir: ./build
                build.temp.dir: ${build.dir}/installer_temp
                build.dist.dir: ${build.dir}/installer_dist
                build.installer.home: ${build.dir}/${installer.name}
                
        2. You can specify script files path on your workspace.
            [default value list]
                properties.dir: ./
            [example]
                installer-maker clean build -properties.dir=./installer-data/                     
    ''')
    void clean(){
        //Setup Log
        setupLog(gOpt.logSetup)

        if (!jobCallingCount++)
            logo()

        logTaskDescription('clean')

        if (!propertiesFile)
            throw new Exception('Does not exists script file [ installer-maker.yml ]')

        try{
            FileMan.delete(gOpt.buildInstallerHome)
        }catch(e){
        }

        try{
            FileMan.delete(gOpt.buildDistDir)
        }catch(e){
        }

        try{
            FileMan.delete(gOpt.buildTempDir)
        }catch(e){
        }
    }

    @Command('build')
    @Document('''
    Build Your Installer
                                                     
          You need 2 Script files(installer-maker.yml, installer.yml)

    - Options
        1. You can change your build directory on Builder Script(installer-maker.yml)  
            [default value list]
                installer.name: installer_myproject                        
                build.dir: ./build
                build.temp.dir: ${build.dir}/installer_temp
                build.dist.dir: ${build.dir}/installer_dist
                build.installer.home: ${build.dir}/${installer.name}
                
        2. You can specify script files path on your workspace.
            [default value list]
                properties.dir: ./
            [example]
                installer-maker clean build -properties.dir=./installer-data/        
    ''')
    void build(){
        //Setup Log
        setupLog(gOpt.logSetup)

        if (!jobCallingCount++)
            logo()

        logTaskDescription('build')

        if (!propertiesFile)
            throw new Exception('Does not exists script file [ installer-maker.yml ]')

        try{
            ReportSetup reportSetup = gOpt.reportSetup

            //1. Gen Starter and Response File
            String binPath = generateLibAndBin(gOpt)

            //- set bin path on builded installer
            provider.setToRawProperty('build.installer.bin.path', binPath)

            //2. Each level by level
            validTaskList = Config.findAllClasses('install', [Task])
            eachTaskWithCommit('build'){ TaskSetup commitTask ->
                try{
                    return runTaskByCommitTask(commitTask)
                }catch(WantToRestartException wtre){
                    throw wtre
                }catch(Exception e){
                    //Write Report
                    writeReport(reportMapList, reportSetup)
                    throw e
                }
            }
            //Write Report
            writeReport(reportMapList, reportSetup)

            // - 1) Make a Response Form
            if (propman.getBoolean('mode.auto.rsp'))
                buildForm()

            // - 2) Zip
            if (propman.getBoolean('mode.auto.zip'))
                zip(gOpt)

            // - 3) Tar
            if (propman.getBoolean('mode.auto.tar'))
                tar(gOpt)

        }catch(e){
            throw e
        }
    }

    @Command('run')
    @HelpIgnore
    @Document("""
    No User's Command        
    """)
    void runCommand(){
        logTaskDescription('RUN')

        if (!propertiesFile)
            throw new Exception('Does not exists script file [ installer-maker.yml ]')


        String binPath = provider.get('build.installer.bin.path') ?: FileMan.getFullPath(gOpt.buildInstallerHome, gOpt.installerHomeToBinRelPath)
        String argsExceptCommand = provider.get('program.args.except.command')
        String argsModeExec = '-mode.exec.self=true'
        String installBinPathForWIn = "${binPath}/installer.bat".replaceAll(/[\/\\]+/, "\\$File.separator")
        String installBinPathForLin = FileMan.toSlash("${binPath}/installer")
        provider.setToRawProperty('command.win', "${installBinPathForWIn} ask install ${argsExceptCommand} ${argsModeExec}")
        provider.setToRawProperty('command.lin', "${installBinPathForLin} ask install ${argsExceptCommand} ${argsModeExec}")
        //run
        runTaskByType('exec')
        //clear
        provider.setToRawProperty('command.win', "")
        provider.setToRawProperty('command.lin', "")
    }

    @Command('test')
    @Document("""
    - Test Command do 'clean' 'build' 'run'.

    - You can use 'test' command to test or build CI Environment.
    
    - Response File(.rsp) can help your test.
         
      installer-maker test -rsp=<File>  
    """)
    void test(){
        config.command( 'clean')
        config.command('build')
        config.command('run')
    }



    void buildForm(){
        provider.propGen.getDefaultProperties().set('mode.build.form', true)
        config.command('form')
    }

    /*************************
     * WRITE Report
     *************************/
    private void writeReport(List reportMapList, ReportSetup reportSetup){
        //Generate File Report
        if (reportMapList){
            String date = new Date().format('yyyyMMdd_HHmmss')
            String fileNamePrefix = 'report_analysis'
            String filePath = reportSetup.fileSetup.path ?: "${fileNamePrefix}_${date}"

            if (reportSetup.modeReportText) {
//                List<String> stringList = sqlman.getAnalysisStringResultList(reportMapList)
//                FileMan.write("${reportSetup.path}.txt", stringList, opt)
            }

            if (reportSetup.modeReportExcel) {
//                new ReportMan().write("${fileNamePrefix}_${date}.xlsx", reportMapList, 'sqlFileName')
//                new ReportMan("${fileNamePrefix}_${date}.xlsx").write('sqlFileName', reportMapList)
                new ReportMan("${filePath}.xlsx").write(reportMapList, 'sqlFileName')
            }
        }

    }



    /*************************
     * Generate Install Starter (Lib, Bin)
     * ${build.installer.home}/lib
     * ${build.installer.home}/bin
     *
     *  1. Create Dir
     *  2. Generate Lib
     *  3. Generate Bin
     *************************/
    private String generateLibAndBin(GlobalOptionForInstallerMaker gOpt){
        //Ready
        FileSetup fileSetup = gOpt.fileSetup
        FileSetup fileSetupForLin = fileSetup.clone([chmod:'755', lineBreak:'\n'])
        String buildTempDir = gOpt.buildTempDir
        String buildDistDir = gOpt.buildDistDir
        String buildInstallerHome = gOpt.buildInstallerHome
        String homeToBinRelPath = gOpt.installerHomeToBinRelPath
        String homeToLibRelPath = gOpt.installerHomeToLibRelPath

        String binDestPath = FileMan.getFullPath(buildInstallerHome, homeToBinRelPath)
        String binToHomeRelPath = FileMan.getRelativePath(binDestPath, buildInstallerHome)
        String binToHomeRelPathForWin = binToHomeRelPath.replace('/','\\')
        String homeToLibRelPathForWin = homeToLibRelPath.replace('/','\\')
        String libDir = provider.getFilePath('lib.dir')
        String libPath = provider.getFilePath('lib.path')
        String thisFileName = FileMan.getLastFileName(libPath)
        String tempNowDir = "${buildTempDir}/temp_${new Date().format('yyyyMMdd_HHmmssSSS')}"
        String libSourcePath = "${libDir}/*.*"
        String libDestPath = FileMan.getFullPath(buildInstallerHome, homeToLibRelPath)
        String libToHomeRelPath = FileMan.getRelativePath(libDestPath, buildInstallerHome)

        /** 1. Convert library for builder to installer**/
        logger.debug """<Installer-Maker> Copy And Generate Installer Library
         - Installer Home: ${buildInstallerHome}"
         - Copy Installer Lib: 
            FROM : ${libSourcePath}
            TO   : ${libDestPath}
        """

        //- Copy Libs
        FileSetup opt = new FileSetup(modeAutoMkdir:true, modeAutoOverWrite:true)
        new FileMan(libSourcePath).set(fileSetup).copy(libDestPath, opt)

        /** 2. Remake Jar(Installer Core) **/
        //- Unjar libs
        FileMan.unjar(libPath, tempNowDir, opt)

        //- Copy Scripts to Installer
        PropMan propmanExternal = provider.propGen.getExternalProperties()
        String userSetPropertiesDir = propman['properties.dir']
        String productVersion = propman['product.version']
        String productName = propman['product.name']

        InstallerMaker builder = config.findInstance(InstallerMaker)
        Installer installer = config.findInstance(Installer)
        Hoya hoya = config.findInstance(Hoya)

        File builderPropertiesFile = FileMan.find(userSetPropertiesDir, builder.propertiesFileName, ["yml", "yaml", "properties"])
        if (builderPropertiesFile)
            FileMan.copy(builderPropertiesFile.path, tempNowDir, opt)
        else
            throw Exception("Does not exist script file(${builder.propertiesFileName})")

        File installerPropertiesFile = FileMan.find(userSetPropertiesDir, installer.propertiesFileName, ["yml", "yaml", "properties"])
        if (installerPropertiesFile)
            FileMan.copy(installerPropertiesFile.path, tempNowDir, opt)
        else
            throw Exception("Does not exist script file(${installer.propertiesFileName})")
        
        File hoyaPropertiesFile = FileMan.find(userSetPropertiesDir, hoya.propertiesFileName, ["yml", "yaml", "properties"])
        if (hoyaPropertiesFile)
            FileMan.copy(hoyaPropertiesFile.path, tempNowDir, opt)



        //- Write 'Relative Path from [lib dir] to [Installer Home dir]'
        FileMan.write("${tempNowDir}/.libtohome", libToHomeRelPath, opt)
        //- Write 'Product Name'
        FileMan.write("${tempNowDir}/.productname", productName, opt)
        //- Write 'Product Version'
        FileMan.write("${tempNowDir}/.productversion", productVersion, opt)

        //- Make jar
        FileMan.jar("${tempNowDir}/*", "${libDestPath}/${thisFileName}", opt)

        /** 3. Generate Runable Binary File (install) **/
        String binInstallShSourcePath = 'binForInstaller/install'
        String binInstallBatSourcePath = 'binForInstaller/install.bat'
        String binInstallShDestPath = "${binDestPath}/install"
        String binInstallBatDestPath = "${binDestPath}/install.bat"
        logger.debug """<Installer-Maker> Generate Bin, install:
            SH  : ${binInstallShDestPath}
            BAT : ${binInstallBatDestPath}
        """

        //- Gen bin/install(sh)
        new FileMan()
        .set(fileSetupForLin)
        .readResource(binInstallShSourcePath)
        .replaceLine([
            'REL_PATH_BIN_TO_HOME=' : "REL_PATH_BIN_TO_HOME=${binToHomeRelPath}",
            'REL_PATH_HOME_TO_LIB=' : "REL_PATH_HOME_TO_LIB=${homeToLibRelPath}"
        ])
        .write(binInstallShDestPath)

        //- Gen bin/install.bat
        new FileMan()
        .set(fileSetup)
        .readResource(binInstallBatSourcePath)
        .replaceLine([
            'set REL_PATH_BIN_TO_HOME=' : "set REL_PATH_BIN_TO_HOME=${binToHomeRelPathForWin}",
            'set REL_PATH_HOME_TO_LIB=' : "set REL_PATH_HOME_TO_LIB=${homeToLibRelPathForWin}"
        ])
        .write(binInstallBatDestPath)

        /** 4. Generate Runable Binary File (installer) **/
        String binInstallerShSourcePath = 'binForInstaller/installer'
        String binInstallerBatSourcePath = 'binForInstaller/installer.bat'
        String binInstallerShDestPath = "${binDestPath}/installer"
        String binInstallerBatDestPath = "${binDestPath}/installer.bat"
        logger.debug """<Installer-Maker> Generate Bin, installer:
            SH  : ${binInstallerShDestPath}
            BAT : ${binInstallerBatDestPath}
        """

        //- Gen bin/installer(sh)
        new FileMan()
        .set(fileSetupForLin)
        .readResource(binInstallerShSourcePath)
        .replaceLine([
            'REL_PATH_BIN_TO_HOME=' : "REL_PATH_BIN_TO_HOME=${binToHomeRelPath}",
            'REL_PATH_HOME_TO_LIB=' : "REL_PATH_HOME_TO_LIB=${homeToLibRelPath}"
        ])
        .write(binInstallerShDestPath)

        //- Gen bin/installer.bat
        new FileMan()
        .set(fileSetup)
        .readResource(binInstallerBatSourcePath)
        .replaceLine([
            'set REL_PATH_BIN_TO_HOME=' : "set REL_PATH_BIN_TO_HOME=${binToHomeRelPathForWin}",
            'set REL_PATH_HOME_TO_LIB=' : "set REL_PATH_HOME_TO_LIB=${homeToLibRelPathForWin}"
        ])
        .write(binInstallerBatDestPath)

        /** 5. Generate Runable Binary File (hoya) **/
        String binHoyaShSourcePath = 'binForInstaller/hoya'
        String binHoyaBatSourcePath = 'binForInstaller/hoya.bat'
        String binHoyaShDestPath = "${binDestPath}/hoya"
        String binHoyaBatDestPath = "${binDestPath}/hoya.bat"
        logger.debug """<Installer-Maker> Generate Bin, Hoya:
            SH  : ${binHoyaShDestPath}
            BAT : ${binHoyaBatDestPath}
        """

        //- Gen bin/hoya(sh)
        new FileMan()
        .set(fileSetupForLin)
        .readResource(binHoyaShSourcePath)
        .replaceLine([
            'REL_PATH_BIN_TO_HOME=' : "REL_PATH_BIN_TO_HOME=${binToHomeRelPath}",
            'REL_PATH_HOME_TO_LIB=' : "REL_PATH_HOME_TO_LIB=${homeToLibRelPath}"
        ])
        .write(binHoyaShDestPath)

        //- Gen bin/hoya.bat
        new FileMan()
        .set(fileSetup)
        .readResource(binHoyaBatSourcePath)
        .replaceLine([
            'set REL_PATH_BIN_TO_HOME=' : "set REL_PATH_BIN_TO_HOME=${binToHomeRelPathForWin}",
            'set REL_PATH_HOME_TO_LIB=' : "set REL_PATH_HOME_TO_LIB=${homeToLibRelPathForWin}"
        ])
        .write(binHoyaBatDestPath)

        /** 6. Generate Runable Binary File (check) **/
        String binCheckShSourcePath = 'binForInstaller/check'
        String binCheckShDestPath = "${binDestPath}/check"
        logger.debug """<Installer-Maker> Generate Bin, check:
            SH  : ${binCheckShDestPath}
        """

        //- Gen bin/hoya(sh)
        new FileMan()
        .set(fileSetupForLin)
        .readResource(binCheckShSourcePath)
        .replaceLine([
            'REL_PATH_BIN_TO_HOME=' : "REL_PATH_BIN_TO_HOME=${binToHomeRelPath}",
            'REL_PATH_HOME_TO_LIB=' : "REL_PATH_HOME_TO_LIB=${homeToLibRelPath}"
        ])
        .write(binCheckShDestPath)

        return binDestPath
    }

    /*************************
     * Distribute ZIP
     * From: ${build.installer.home}
     *   To: ${build.dist.dir}
     *************************/
    void zip(GlobalOptionForInstallerMaker gOpt){
        //Ready
        FileSetup fileSetup = gOpt.fileSetup
        String installerName = gOpt.installerName
        String buildTempDir = gOpt.buildTempDir
        String buildDistDir = gOpt.buildDistDir
        String buildInstallerHome = gOpt.buildInstallerHome

        //Log
        logTaskDescription('auto zip')

        //Zip
        FileMan.zip(buildInstallerHome, "${buildDistDir}/${installerName}.zip", fileSetup)
    }

    /*************************
     * Distribute TAR
     * From: ${build.installer.home}
     *   To: ${build.dist.dir}
     *************************/
    void tar(GlobalOptionForInstallerMaker gOpt){
        //Ready
        FileSetup fileSetup = gOpt.fileSetup
        String installerName = gOpt.installerName
        String buildTempDir = gOpt.buildTempDir
        String buildDistDir = gOpt.buildDistDir
        String buildInstallerHome = gOpt.buildInstallerHome

        //Log
        logTaskDescription('auto tar')

        //Zip
        FileMan.tar(buildInstallerHome, "${buildDistDir}/${installerName}.tar", fileSetup)
    }



}
