// src/main/java/com/example/redchina/service/StoryScriptService.java
package com.example.redchina.service;

import com.alibaba.fastjson.JSONObject;
import com.example.redchina.entity.SysStory;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class StoryScriptService {

    @Resource
    private SysStoryService storyService;
    @Resource
    private SysStoryNodeService nodeService;
    @Resource
    private SysStoryOptionService optionService;

    // 从JSON脚本初始化剧情
    public void initStoryFromScript(String scriptName) throws Exception {
        // 读取JSON文件
        ClassPathResource resource = new ClassPathResource("story-scripts/" + scriptName + ".json");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        JSONObject json = JSONObject.parseObject(sb.toString());

        // 保存剧情
        SysStory story = new SysStory();
        story.setStoryTitle(json.getString("title"));
        story.setStoryIntro(json.getString("intro"));
        story.setTagId(json.getLong("tagId"));
        story.setStatus(1);
        storyService.save(story);

        // 保存节点和选项（省略具体JSON解析逻辑，根据实际格式实现）
        List<JSONObject> nodes = json.getJSONArray("nodes").toJavaList(JSONObject.class);
        for (JSONObject nodeJson : nodes) {
            // 保存节点和选项的逻辑
        }
    }
}
