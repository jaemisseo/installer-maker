package install.task

import install.util.TaskUtil
import jaemisseo.man.FileMan
import jaemisseo.man.SqlAnalMan
import jaemisseo.man.SqlMan
import jaemisseo.man.util.SqlSetup
import install.bean.ReportSetup
import install.bean.ReportSql

import java.sql.SQLException

/**
 * Created by sujkim on 2017-02-17.
 */
class Sql extends TaskUtil{

    @Override
    Integer run(){

        //1. Default Setup
        sqlman = new SqlMan()
        SqlSetup sqlSetup = genMergedSqlSetup()
        List<String> filePathList = getFilePathList('file.path', 'sql')
        ReportSetup reportSetup = genGlobalReportSetup()


        //2. Execute All SQL
        println "<SQL>"

        filePathList.each{ String filePath ->

            String originFileName = new File(filePath).getName()

            //2. Generate Query Replaced With New Object Name
            sqlman.init()
                    .queryFromFile(filePath)
                    .command([SqlMan.ALL])
                    .replace(sqlSetup)

            //3. Report Checking Before
            if (sqlSetup.modeSqlCheckBefore){
                try {
                    sqlman.checkBefore(sqlSetup)

                }catch(e){
                    println "<ERROR> Checking Before Execution"
                    throw new SQLException('Error, Checking Before Execution.')
                }
            }

            //- Generate SQL File
            if (sqlSetup.modeSqlFileGenerate){
                println "Creating SQL File..."
                FileMan.write("./replaced_${originFileName}", sqlman.getReplacedQueryList(), reportSetup.fileSetup)
            }

            //4. Execute
            if (sqlSetup.modeSqlExecute){
                sqlman.run(sqlSetup)
            }

        }

        return STATUS_TASK_DONE
    }

    @Override
    void reportWithConsole(ReportSetup reportSetup, List reportMapList){
        sqlman.reportResult()
    }

    @Override
    void reportWithText(ReportSetup reportSetup, List reportMapList){

    }

    @Override
    void reportWithExcel(ReportSetup reportSetup, List reportMapList){
//        Map resultMap = sqlman.getResultReportMap()
        sqlman.getAnalysisResultList().each{ SqlAnalMan.SqlObject sqlObj ->
            reportMapList.add(new ReportSql(
                    sqlFileName: sqlObj.sqlFileName,
                    seq: sqlObj.seq,
                    query: sqlObj.query,
//                    isExistOnDB     : sqlObj.isExistOnDB?'Y':'N',
                    isOk: (sqlObj.isOk == null) ? '' : (sqlObj.isOk) ? 'Complete' : 'Failed',
                    warnningMessage: sqlObj.warnningMessage,
                    error: sqlObj.error?.toString(),
            ))
        }
    }



}