package com.assistant.ai.rag;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 中文增强文本分块器  继承TextSplitter抽象类
 * 优化中文标点支持，提供更准确的中文文本分块
 * 分块标识符按需更新 CHINESE_SEPARATORS
 *
 * @author endcy
 * @date 2025/10/23 20:42:17
 */
public class ChineseEnhancedTextSplitter extends TextSplitter {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_MIN_CHUNK_SIZE_CHARS = 100;
    private static final int DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int DEFAULT_MAX_NUM_CHUNKS = 10000;
    private static final boolean DEFAULT_KEEP_SEPARATOR = true;

    // 中文标点分隔符（优先级从高到低），先移除英文的. 规避分割异常
    private static final List<String> CHINESE_SEPARATORS = Arrays.asList(
            "。", "！", "？", "\n\n", "\n", "；", "，", "!", "\\?", "  ", "\t", "."
    );

    private final EncodingRegistry registry;
    private final Encoding encoding;
    private final int chunkSize;
    private final int minChunkSizeChars;
    private final int minChunkLengthToEmbed;
    private final int maxNumChunks;
    private final boolean keepSeparator;

    public ChineseEnhancedTextSplitter() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_MIN_CHUNK_SIZE_CHARS,
                DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED, DEFAULT_MAX_NUM_CHUNKS,
                DEFAULT_KEEP_SEPARATOR);
    }

    public ChineseEnhancedTextSplitter(boolean keepSeparator) {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_MIN_CHUNK_SIZE_CHARS, DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED, DEFAULT_MAX_NUM_CHUNKS, keepSeparator);
    }

    public ChineseEnhancedTextSplitter(int chunkSize, int minChunkSizeChars,
                                       int minChunkLengthToEmbed, int maxNumChunks,
                                       boolean keepSeparator) {
        this.registry = Encodings.newLazyEncodingRegistry();
        this.encoding = this.registry.getEncoding(EncodingType.CL100K_BASE);
        this.chunkSize = chunkSize;
        this.minChunkSizeChars = minChunkSizeChars;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks;
        this.keepSeparator = keepSeparator;
    }

    /**
     * 核心方法：将文本分割成块
     */
    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> tokens = getEncodedTokens(text);
        List<String> chunks = new ArrayList<>();
        int numChunks = 0;

        while (!tokens.isEmpty() && numChunks < this.maxNumChunks) {
            // 获取当前块的最大token数量
            List<Integer> chunkTokens = tokens.subList(0, Math.min(this.chunkSize, tokens.size()));
            String chunkText = decodeTokens(chunkTokens);

            if (chunkText.trim().isEmpty()) {
                tokens = tokens.subList(chunkTokens.size(), tokens.size());
                continue;
            }

            // 查找最合适的中文分隔点
            int splitPoint = findBestChineseSplitPoint(chunkText);

            String finalChunkText;
            if (splitPoint != -1 && splitPoint >= this.minChunkSizeChars) {
                // 在合适的分隔点分割
                finalChunkText = chunkText.substring(0, splitPoint + 1);
            } else {
                // 没有找到合适分隔点或分隔后太小，使用原始块
                finalChunkText = chunkText;
            }

            // 处理分隔符和空白字符
            String chunkToAppend = this.keepSeparator ?
                    finalChunkText.trim() : finalChunkText.replace(System.lineSeparator(), " ").trim();

            // 检查块长度是否满足最小要求
            if (chunkToAppend.length() >= this.minChunkLengthToEmbed) {
                chunks.add(chunkToAppend);
                numChunks++;
            }

            // 计算已处理token数量并移除
            int processedTokens = getEncodedTokens(finalChunkText).size();
            if (processedTokens > 0) {
                tokens = tokens.subList(processedTokens, tokens.size());
            } else {
                // 防止无限循环
                break;
            }
        }

        // 处理剩余的token
        if (!tokens.isEmpty()) {
            String remainingText = decodeTokens(tokens)
                    .replace(System.lineSeparator(), " ").trim();
            if (remainingText.length() >= this.minChunkLengthToEmbed) {
                chunks.add(remainingText);
            }
        }

        return chunks;
    }

    /**
     * 查找最佳的中文分割点
     */
    private int findBestChineseSplitPoint(String text) {
        int bestSplitPoint = -1;

        // 按优先级查找中文分隔符
        for (String separator : CHINESE_SEPARATORS) {
            int lastIndex = text.lastIndexOf(separator);
            if (lastIndex > bestSplitPoint) {
                bestSplitPoint = lastIndex;
                // 如果是多字符分隔符，需要调整位置
                if (separator.length() > 1) {
                    bestSplitPoint += separator.length() - 1;
                }
            }
        }

        // 确保分割点不会导致块过小
        if (bestSplitPoint != -1 && bestSplitPoint < this.minChunkSizeChars) {
            // 如果分割后块太小，尝试向前查找更大的分隔点
            for (String separator : CHINESE_SEPARATORS) {
                int currentIndex = bestSplitPoint;
                while (currentIndex != -1) {
                    int nextIndex = text.lastIndexOf(separator, currentIndex - 1);
                    if (nextIndex == -1)
                        break;

                    if (nextIndex >= this.minChunkSizeChars) {
                        bestSplitPoint = nextIndex;
                        if (separator.length() > 1) {
                            bestSplitPoint += separator.length() - 1;
                        }
                        break;
                    }
                    currentIndex = nextIndex;
                }
            }
        }

        return bestSplitPoint;
    }

    /**
     * 将文本编码为token列表
     */
    private List<Integer> getEncodedTokens(String text) {
        Assert.notNull(text, "Text must not be null");
        return this.encoding.encode(text).boxed();
    }

    /**
     * 将token列表解码为文本
     */
    private String decodeTokens(List<Integer> tokens) {
        Assert.notNull(tokens, "Tokens must not be null");
        IntArrayList tokensIntArray = new IntArrayList(tokens.size());
        tokens.forEach(tokensIntArray::add);
        return this.encoding.decode(tokensIntArray);
    }

    /**
     * 应用分块到文档列表
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            List<String> chunks = splitText(doc.getText());
            for (String chunk : chunks) {
                // 复制原始文档的元数据到新块
                Document chunkDoc = new Document(chunk, doc.getMetadata());
                result.add(chunkDoc);
            }
        }
        return result;
    }

    // Builder模式用于链式配置
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private int minChunkSizeChars = DEFAULT_MIN_CHUNK_SIZE_CHARS;
        private int minChunkLengthToEmbed = DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED;
        private int maxNumChunks = DEFAULT_MAX_NUM_CHUNKS;
        private boolean keepSeparator = DEFAULT_KEEP_SEPARATOR;

        public Builder withChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder withMinChunkSizeChars(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;
            return this;
        }

        public Builder withMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;
            return this;
        }

        public Builder withMaxNumChunks(int maxNumChunks) {
            this.maxNumChunks = maxNumChunks;
            return this;
        }

        public Builder withKeepSeparator(boolean keepSeparator) {
            this.keepSeparator = keepSeparator;
            return this;
        }

        public ChineseEnhancedTextSplitter build() {
            return new ChineseEnhancedTextSplitter(
                    chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
                    maxNumChunks, keepSeparator
            );
        }
    }
}
