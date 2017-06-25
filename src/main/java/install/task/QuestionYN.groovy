package install.task

import install.util.TaskUtil
import jaemisseo.man.QuestionMan
import jaemisseo.man.util.QuestionSetup

/**
 * Created by sujkim on 2017-03-18.
 */
class QuestionYN extends TaskUtil{

    @Override
    Integer run(){

        //Get Properties
        qman = new QuestionMan().setValidAnswer([undoSign, redoSign])
        QuestionSetup opt = genMergedQuestionSetup()

        //Ask Question
        //Get Answer
        String yourAnswer = qman.question(opt, QuestionMan.QUESTION_TYPE_YN)

        //Check undo & redo command
        if (checkUndoQuestion(yourAnswer))
            return STATUS_UNDO_QUESTION
        else if (checkRedoQuestion(yourAnswer))
            return STATUS_REDO_QUESTION

        //Remeber Answer
        rememberAnswerLineList.add("${propertyPrefix}answer.default=${yourAnswer}")

        //Set 'answer' and 'value' Property
        set('answer', yourAnswer)
        set('value', qman.getValue())

        //If 'answer' is Y
        if (yourAnswer?.toUpperCase()?.equals(QuestionMan.Y))
            setPropValue()

        return STATUS_TASK_DONE
    }



    /**
     * BUILD FORM
     */
    List<String> buildForm(String propertyPrefix){
        this.propertyPrefix = propertyPrefix
        qman = new QuestionMan().setValidAnswer([undoSign, redoSign])
        QuestionSetup opt = genMergedQuestionSetup()

        return (!opt.modeOnlyInteractive) ? qman.genQuestion(opt) : []
    }

}