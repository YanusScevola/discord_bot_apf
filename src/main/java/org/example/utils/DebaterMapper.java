package org.example.utils;

import net.dv8tion.jda.api.entities.Member;
import org.example.core.models.DebaterAPF;

import java.util.List;
import java.util.stream.Collectors;

public class DebaterMapper {

    public static DebaterAPF mapFromMember(Member member) {
        DebaterAPF debaterAPF = new DebaterAPF();

//        debaterAPF.setMemberId(member.getId());
        debaterAPF.setNickname(member.getUser().getName());
//        debaterAPF.setBalls(0);
//        debaterAPF.setDebateCount(0);
//        debaterAPF.setWinner(0);

        return debaterAPF;
    }
    public static List<DebaterAPF> mapFromMembers(List<Member> members) {
        return members.stream().map(DebaterMapper::mapSingleMemberToDebater).collect(Collectors.toList());
    }

    private static DebaterAPF mapSingleMemberToDebater(Member member) {
        DebaterAPF debaterAPF = new DebaterAPF();

//        debaterAPF.setMemberId(member.getId());
        debaterAPF.setNickname(member.getUser().getName());
//        debaterAPF.setBalls(0);
//        debaterAPF.setDebateCount(0);
//        debaterAPF.setWinner(0);

        return debaterAPF;
    }
}
