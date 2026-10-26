package vn.edu.ueh.nhom4.grandmaassist.app.domain.service;

import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SearchPreference;

public final class GuidedJobMatcher {

    public static final class MatchResult {
        public final PublicJob job;
        public final int score;
        public final List<String> explanationTags;

        public MatchResult(PublicJob job, int score, List<String> tags) {
            this.job = job;
            this.score = score;
            this.explanationTags = tags != null ? Collections.unmodifiableList(new ArrayList<>(tags)) : Collections.emptyList();
        }
    }

    public static List<MatchResult> evaluate(List<PublicJob> jobs, SearchPreference pref, String seekerProfileArea) {
        if (jobs == null || jobs.isEmpty()) return Collections.emptyList();
        if (pref == null) pref = new SearchPreference(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0);

        List<MatchResult> results = new ArrayList<>();
        for (PublicJob job : jobs) {
            if (job == null) continue;

            // Hard Constraints
            if (!pref.areaCodes.isEmpty() && !pref.areaCodes.contains(job.areaCode)) continue;

            if (!pref.avoidedDemands.isEmpty() && job.demands != null) {
                boolean hasAvoided = false;
                for (String d : job.demands) {
                    if (pref.avoidedDemands.contains(d)) { hasAvoided = true; break; }
                }
                if (hasAvoided) continue;
            }

            if (!pref.days.isEmpty() && !"FLEXIBLE".equals(job.shift) && !pref.isShiftFlexible()) {
                boolean intersect = false;
                if (job.days != null) {
                    for (String d : job.days) {
                        if (pref.days.contains(d)) { intersect = true; break; }
                    }
                }
                if (!intersect) continue;
            }

            if (!pref.shifts.isEmpty() && !"FLEXIBLE".equals(job.shift) && !pref.isShiftFlexible()) {
                if (!pref.shifts.contains(job.shift)) continue;
            }

            // Soft Constraints (Scoring max 100)
            int score = 0;
            if (pref.categoryIds.isEmpty()) {
                score += 30;
            } else if (pref.hasCategory(job.categoryId)) {
                score += 50;
            }

            if ("FLEXIBLE".equals(job.shift) || pref.isShiftFlexible()) {
                score += 15;
            } else {
                score += 30;
            }

            if (seekerProfileArea != null && !seekerProfileArea.isEmpty() && job.areaCode.equals(seekerProfileArea)) {
                score += 20;
            }

            // Explanation Tags
            List<String> tags = new ArrayList<>();
            if (pref.hasCategory(job.categoryId)) {
                tags.add("Đúng loại việc đã chọn");
            }

            if (seekerProfileArea != null && !seekerProfileArea.isEmpty() && job.areaCode.equals(seekerProfileArea)) {
                tags.add("Gần khu vực hồ sơ");
            } else if (job.areaName != null && !job.areaName.isEmpty()) {
                tags.add("Khu vực " + job.areaName);
            }

            if ("FLEXIBLE".equals(job.shift)) {
                tags.add("Cần trao đổi lịch");
            } else {
                String s = job.shift;
                if ("MORNING".equalsIgnoreCase(s)) s = "sáng";
                else if ("AFTERNOON".equalsIgnoreCase(s)) s = "chiều";
                else if ("EVENING".equalsIgnoreCase(s)) s = "tối";
                tags.add("Làm buổi " + s);
            }

            if (job.demands == null || job.demands.isEmpty() || !job.demands.contains("HEAVY_LIFTING")) {
                tags.add("Không yêu cầu mang vác nặng");
            }

            results.add(new MatchResult(job, score, tags));
        }

        results.sort((a, b) -> a.score != b.score ? Integer.compare(b.score, a.score) : Long.compare(a.job.expiresAt, b.job.expiresAt));
        return results;
    }
}
