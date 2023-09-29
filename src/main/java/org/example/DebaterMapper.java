package org.example;

import net.dv8tion.jda.api.entities.Member;
import org.example.models.Debater;

import java.util.List;
import java.util.stream.Collectors;

public class DebaterMapper {

    public static Debater mapFromMember(Member member) {
        Debater debater = new Debater();

        debater.setId(member.getId());
        debater.setNickname(member.getUser().getName());
        debater.setTeamName("нет информации");
        debater.setBalls(0);
        debater.setDebateCount(0);
        debater.setWinner(0);

        return debater;
    }
    public static List<Debater> mapFromMembers(List<Member> members) {
        return members.stream().map(DebaterMapper::mapSingleMemberToDebater).collect(Collectors.toList());
    }

    private static Debater mapSingleMemberToDebater(Member member) {
        Debater debater = new Debater();

        debater.setId(member.getId());
        debater.setNickname(member.getUser().getName());
        debater.setTeamName("нет информации");
        debater.setBalls(0);
        debater.setDebateCount(0);
        debater.setWinner(0);

        return debater;
    }
}
