package org.upperlevel.corrida.phase.game.playing;

import org.upperlevel.corrida.command.Command;

import lombok.Getter;

public class SuggestCommand extends Command {
    @Getter
    private String description;

    @Getter
    private String question;

    @Getter
    private String answer;

    public SuggestCommand() {
    }

    public SuggestCommand(String description, String question, String answer) {
        this.description = description;
        this.question = question;
        this.answer = answer;
    }

    public boolean hasQuestion() {
        return question != null && !question.isEmpty();
    }

    public boolean hasAnswer() {
        return answer != null && !answer.isEmpty();
    }

    @Override
    public String encode() {
        return "suggest \"" + description + "\" \"" + question + "\" \"" + answer + "\"\n";
    }

    @Override
    public SuggestCommand decode(String[] split) {
        // suggest <description>
        description = split[1];
        // suggest <description> [question]
        if (split.length > 2) {
            question = split[2];
        }
        // suggest <description> [question] [answer]
        if (split.length > 3) {
            answer = split[3];
        }
        return this;
    }
}
