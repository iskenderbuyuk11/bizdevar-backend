package com.bizdevar.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
public class HomeStoryController {

    private final HomeStoryRepository stories;

    public HomeStoryController(HomeStoryRepository stories) {
        this.stories = stories;
    }

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", stories.listActive());
        return out;
    }
}
