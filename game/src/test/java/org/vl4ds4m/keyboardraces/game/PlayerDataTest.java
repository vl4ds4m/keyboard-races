package org.vl4ds4m.keyboardraces.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class PlayerDataTest {
    private final List<PlayerData> actual = new ArrayList<>();
    private List<PlayerData> expected;

    @BeforeEach
    public void createPlayers() {
        PlayerData p1 = new PlayerData("p1");
        actual.add(p1);
        p1.setConnected(true);
        p1.setFinishTime(-1);
        p1.setInputCharsCount(20);
        p1.setErrorsCount(2);

        PlayerData p2 = new PlayerData("p2");
        actual.add(p2);
        p2.setConnected(true);
        p2.setFinishTime(-1);
        p2.setInputCharsCount(20);
        p2.setErrorsCount(1);

        PlayerData p3 = new PlayerData("p3");
        actual.add(p3);
        p3.setConnected(true);
        p3.setFinishTime(-1);
        p3.setInputCharsCount(10);
        p3.setErrorsCount(2);

        PlayerData p4 = new PlayerData("p4");
        actual.add(p4);
        p4.setConnected(true);
        p4.setFinishTime(-1);
        p4.setInputCharsCount(10);
        p4.setErrorsCount(1);

        PlayerData p5 = new PlayerData("p5");
        actual.add(p5);
        p5.setConnected(true);
        p5.setFinishTime(5);
        p5.setInputCharsCount(10);
        p5.setErrorsCount(2);

        PlayerData p6 = new PlayerData("p6");
        actual.add(p6);
        p6.setConnected(true);
        p6.setFinishTime(10);
        p6.setInputCharsCount(10);
        p6.setErrorsCount(2);

        PlayerData p7 = new PlayerData("p7");
        actual.add(p7);
        p7.setConnected(false);
        p7.setFinishTime(7);
        p7.setInputCharsCount(10);
        p7.setErrorsCount(2);

        PlayerData p8 = new PlayerData("p8");
        actual.add(p8);
        p8.setConnected(false);
        p8.setFinishTime(12);
        p8.setInputCharsCount(10);
        p8.setErrorsCount(2);

        PlayerData p9 = new PlayerData("p9");
        actual.add(p9);
        p9.setConnected(false);
        p9.setFinishTime(-1);
        p9.setInputCharsCount(20);
        p9.setErrorsCount(1);

        expected = List.of(p8, p6, p7, p5, p2, p1, p4, p3, p9);
    }

    @Test
    public void testComparator() {
        Assertions.assertEquals(expected.size(), actual.size());

        actual.sort(PlayerData.RATE_COMP);

        for (int i = 0; i < expected.size(); ++i) {
            Assertions.assertEquals(expected.get(i).getName(), actual.get(i).getName());
        }
    }
}
