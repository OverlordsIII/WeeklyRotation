package io.github.overlordsiii.ranking;

import java.util.*;

import static io.github.overlordsiii.Main.*;

public class RankingProgram {

    // TODO output ranked list of objects for further user use (like creating playlist)
    public static <T> void rank(List<RankedObject<T>> list) {
        Scanner scanner = new Scanner(System.in);

        while (!areDistinctPrioritiesInList(list)) {
            List<Pair<RankedObject<T>>> pairs = splitIntoPairs(list);


            for (Pair<RankedObject<T>> pair : pairs) {
                LOGGER.info("Choose 1 or 2 between there two:");
                LOGGER.info(pair.getO1().getName());
                LOGGER.info(pair.getO2().getName());
                int i = scanner.nextInt();
                if (i == 1) {
                    pair.getO1().incrementPriority();
                } else if (i == 2) {
                    pair.getO2().incrementPriority();
                } else {
                    throw new IllegalArgumentException("Wrong input! Must choose 1 or 2!");
                }
            }
            list = decompressPairs(pairs);
            sortListByPriority(list);
        }
    }

    private static <T> boolean areDistinctPrioritiesInList(List<RankedObject<T>> list) {
        List<Integer> priorities = new ArrayList<>();

        for (RankedObject<T> tRankedObject : list) {
            if (priorities.contains(tRankedObject.getPriority())) {
                return false;
            }
            priorities.add(tRankedObject.getPriority());
        }

        return true;
    }

    private static <T> void sortListByPriority(List<RankedObject<T>> list) {
        list.sort(Comparator.comparingInt(RankedObject::getPriority));
    }

    private static <T> List<T> decompressPairs(List<Pair<T>> pairs) {
        List<T> list = new ArrayList<>();
        pairs.forEach(pair -> {
            list.add(pair.getO1());
            list.add(pair.getO2());
        });

        return list;
    }


    private static <T> List<Pair<RankedObject<T>>> splitIntoPairs(List<RankedObject<T>> list) {
        List<Pair<RankedObject<T>>> newList = new ArrayList<>();

        for (int i = 0; i < list.size() - 1; i++) {
            newList.add(new Pair<>(list.get(i), list.get(i + 1)));
        }

        return newList;
    }


}
