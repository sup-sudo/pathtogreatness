package dev.supreethranganathan.pathtogreatness1.model;


import java.util.List;
public record TavilyResponse(List<TavilyResult> results, String query, String answer) {
}
