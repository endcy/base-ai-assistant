package com.endcy.ai.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于文件持久化的对话记忆。
 * <p>
 * 线程安全说明：
 * <ul>
 *   <li>Kryo 实例非线程安全，每次序列化通过 {@link #newKryo()} 创建局部实例；</li>
 *   <li>同一 conversationId 的读-改-写操作通过会话级 {@link ReentrantLock} 串行化，
 *       避免并发写入互相覆盖丢失消息。</li>
 * </ul>
 *
 * @author endcy
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    /**
     * 会话级锁表：conversationId -> 锁，保证同一会话的读写互斥
     */
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    private final String BASE_DIR;

    // 构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<Message> conversationMessages = getOrCreateConversation(conversationId);
            conversationMessages.addAll(messages);
            saveConversation(conversationId, conversationMessages);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 创建新的 Kryo 实例（Kryo 非线程安全，不复用共享单例）。
     */
    private static Kryo newKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        // 设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = newKryo().readObject(input, ArrayList.class);
            } catch (IOException e) {
                log.error("读取会话记录失败, conversationId={}", conversationId, e);
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            newKryo().writeObject(output, messages);
        } catch (IOException e) {
            log.error("保存会话记录失败, conversationId={}", conversationId, e);
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
