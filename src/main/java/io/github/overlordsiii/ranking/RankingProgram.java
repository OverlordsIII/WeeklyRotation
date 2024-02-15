package io.github.overlordsiii.ranking;

import java.util.*;

import static io.github.overlordsiii.Main.*;

public class RankingProgram {

    // TODO output ranked list of objects for further user use (like creating playlist)
    public static <T> void rank(List<RankedObject<T>> list) {
        Scanner scanner = new Scanner(System.in);
        int numQuestions = list.size() - 1;
        Collections.shuffle(list);

        for (int i = 0; i < list.size(); i++) {
            for (int j = i+1; j < list.size(); j++) {
                RankedObject<T> s1 = list.get(i);
                RankedObject<T> s2 = list.get(j);
                LOGGER.info("What song is better?");
                LOGGER.info("1: " + s1.getName());
                LOGGER.info("2: " + s2.getName());
                int choice = scanner.nextInt();
                if (choice == 1) {
                    list.get(i).incrementPriority(numQuestions - j + i);
                    list.get(j).decrementPriority(numQuestions - j + i);
                } else if (choice == 2) {
                    list.get(i).decrementPriority(numQuestions - j + i);
                    list.get(j).incrementPriority(numQuestions - j + i);
                }
            }
        }

        list.sort((s1, s2) -> {
            int scoreDiff = s2.getPriority() - s1.getPriority();
            if (scoreDiff != 0) {
                return scoreDiff;
            } else {
                // If the scores are equal, ask the user to break the tie
                LOGGER.info("There is a tie between " + s1 + " and " + s2 + ". Please choose one:");
                LOGGER.info("1. " + s1.getName());
                LOGGER.info("2. " + s2.getName());
                int choice = scanner.nextInt();
                if (choice == 1) {
                    return -1;
                } else if (choice == 2) {
                    return 1;
                } else {
                    // If the user enters an invalid choice, treat it as a tie
                    return 0;
                }
            }
        });
        for (int i = 0; i < list.size(); i++) {
            LOGGER.info((i+1) + ": " + list.get(i).getName());
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
/*
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

        for (int i = 0; i < list.size() - 1; i += 2) {
             newList.add(new Pair<>(list.get(i), list.get(i + 1)));
        }

        return newList;
    }

 */


}
