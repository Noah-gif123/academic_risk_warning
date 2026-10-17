package Yyxtao;

import java.util.*;

public class ScoreSystem {
    private Map<Integer, AthleteScore> athleteScores;

    public ScoreSystem() {
        athleteScores = new HashMap<>();
    }

    // 提供按照编号查询的功能
    public AthleteScore getAthleteById(int id) {
        return athleteScores.get(id);
    }

    // 提供指定数值分量区间查询的功能，查询结果应升序或降序排列
    public List<AthleteScore> queryScoresInRange(double start, double end, boolean ascending) {
        List<AthleteScore> results = new ArrayList<>();
        for (AthleteScore athleteScore : athleteScores.values()) {
            if (athleteScore.getScore() >= start && athleteScore.getScore() <= end) {
                results.add(athleteScore);
            }
        }
        if (ascending) {
            results.sort(Comparator.comparingDouble(AthleteScore::getScore));
        } else {
            results.sort(Comparator.comparingDouble(AthleteScore::getScore).reversed());
        }
        return results;
    }

    // 提供分类统计功能
    public Map<String, Integer> getCategoryStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (AthleteScore athleteScore : athleteScores.values()) {
            stats.put(athleteScore.getCategory(), stats.getOrDefault(athleteScore.getCategory(), 0) + 1);
        }
        return stats;
    }

    // 提供排行榜或类似功能
    public List<AthleteScore> getTopAthletes(int topN, boolean ascending) {
        List<AthleteScore> scores = new ArrayList<>(athleteScores.values());
        scores.sort(Comparator.comparingDouble(AthleteScore::getScore));
        if (!ascending) {
            Collections.reverse(scores);
        }
        return scores.subList(0, Math.min(topN, scores.size()));
    }

    // 提供信息录入、修改和输出功能
    public void addOrUpdateAthlete(AthleteScore athleteScore) {
        athleteScores.put(athleteScore.getId(), athleteScore);
    }

    public void removeAthlete(int id) {
        athleteScores.remove(id);
    }

    public void printAllAthletes() {
        for (AthleteScore athleteScore : athleteScores.values()) {
            System.out.println(athleteScore);
        }
    }
}


