package com.zhaoxinms.contract.tools.aicomponent.util;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.FileCreateParams;
import com.openai.models.FileObject;
import com.openai.models.FilePurpose;
 
public class Test {
	
	public static void main1(String args[]) {
		// 创建客户端，使用环境变量中的API密钥
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey("sk-3e160de89efd4862923a24e22e72ed08")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
        // 设置文件路径,请根据实际需求修改路径与文件名
        Path filePath = Paths.get("C:\\Users\\91088\\Desktop\\新建 XLS 工作表.pdf");
        // 创建文件上传参数
        FileCreateParams params = FileCreateParams.builder()
                .file(filePath)
                .purpose(FilePurpose.of("file-extract"))
                .build();

        // 上传文件
        FileObject fileObject = client.files().create(params);
        System.out.println(fileObject);
	}
	
	public static void main(String[] args) {
        // 创建客户端，使用环境变量中的API密钥
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey("sk-3e160de89efd4862923a24e22e72ed08")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();

        // 创建聊天请求
        ChatCompletionCreateParams chatParams = ChatCompletionCreateParams.builder()
                .addSystemMessage("You are a helpful assistant.")
                //请将 'file-fe-xxx'替换为您实际对话场景所使用的 fileid。
                .addSystemMessage("fileid://file-fe-e881f70c80bd474e98f0053b")
                .addUserMessage("这篇文章讲了什么？")
                .model("qwen-long")
                .build();

        StringBuilder fullResponse = new StringBuilder();

        // 所有代码示例均采用流式输出，以清晰和直观地展示模型输出过程。如果您希望查看非流式输出的案例，请参见https://help.aliyun.com/zh/model-studio/text-generation
        try (StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(chatParams)) {
            streamResponse.stream().forEach(chunk -> {
                // 打印每个 chunk 的内容并拼接
                System.out.println(chunk);
                String content = chunk.choices().get(0).delta().content().orElse("");
                if (!content.isEmpty()) {
                    fullResponse.append(content);
                }
            });
            System.out.println(fullResponse);
        } catch (Exception e) {
            System.err.println("错误信息：" + e.getMessage());
            System.err.println("请参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code");
        }
    }

}
