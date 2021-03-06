package com.ncu.xzx.mapper;

import com.ncu.xzx.model.ChoiceQuestion;
import com.ncu.xzx.model.FileDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChoiceQuestionMapper {

    int addChoiceQuestion(ChoiceQuestion choiceQuestion);

    List<ChoiceQuestion> getChoiceQuestionByPage(int offset, int pageSize);

    List<ChoiceQuestion> getByUserId(int userId);

    int countAllChoiceQuestions();

    int updateChoiceQuestion(ChoiceQuestion choiceQuestion);

    List<ChoiceQuestion> getByDescription(String description);

    ChoiceQuestion getById(int id);

    ChoiceQuestion getMaxId();
}
