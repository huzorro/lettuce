// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.pubsub.*;
import org.junit.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static junit.framework.Assert.assertEquals;

public class PubSubCommandTest extends AbstractCommandTest implements RedisPubSubListener<String> {
    private RedisPubSubConnection<String, String> pubsub;

    private BlockingQueue<String> channels;
    private BlockingQueue<String> patterns;
    private BlockingQueue<String> messages;
    private BlockingQueue<Long> counts;

    private String channel = "channel0";
    private String pattern = "channel*";
    private String message = "msg!";

    @Before
    public void openPubSubConnection() throws Exception {
        pubsub = client.connectPubSub();
        pubsub.addListener(this);
        channels = new LinkedBlockingQueue<String>();
        patterns = new LinkedBlockingQueue<String>();
        messages = new LinkedBlockingQueue<String>();
        counts   = new LinkedBlockingQueue<Long>();
    }

    @After
    public void closePubSubConnection() throws Exception {
        pubsub.close();
    }

    @Test
    public void auth() throws Exception {
        RedisClient client = new RedisClient(authHost, authPort);
        RedisPubSubConnection<String, String> connection = client.connectPubSub();
        connection.addListener(this);
        connection.auth(passwd);
        connection.subscribe(channel);
        assertEquals(channel, channels.take());
        connection.close();
    }

    @Test(timeout = 100)
    public void message() throws Exception {
        pubsub.subscribe(channel);
        assertEquals(channel, channels.take());

        redis.publish(channel, message);
        assertEquals(channel, channels.take());
        assertEquals(message, messages.take());
    }

    @Test(timeout = 100)
    public void pmessage() throws Exception {
        pubsub.psubscribe(pattern);
        assertEquals(pattern, patterns.take());

        redis.publish(channel, message);
        assertEquals(pattern, patterns.take());
        assertEquals(channel, channels.take());
        assertEquals(message, messages.take());

        redis.publish("channel2", "msg 2!");
        assertEquals(pattern, patterns.take());
        assertEquals("channel2", channels.take());
        assertEquals("msg 2!", messages.take());
    }

    @Test(timeout = 100)
    public void psubscribe() throws Exception {
        pubsub.psubscribe(pattern);
        assertEquals(pattern, patterns.take());
        assertEquals(1, (long) counts.take());
    }

    @Test(timeout = 100)
    public void punsubscribe() throws Exception {
        pubsub.punsubscribe(pattern);
        assertEquals(pattern, patterns.take());
        assertEquals(0, (long) counts.take());
    }

    @Test(timeout = 100)
    public void subscribe() throws Exception {
        pubsub.subscribe(channel);
        assertEquals(channel, channels.take());
        assertEquals(1, (long) counts.take());
    }

    @Test(timeout = 100)
    public void unsubscribe() throws Exception {
        pubsub.unsubscribe(channel);
        assertEquals(channel, channels.take());
        assertEquals(0, (long) counts.take());
    }

    @Test(timeout = 1000)
    public void resubscribeOnReconnect() throws Exception {
        pubsub.subscribe(channel);
        assertEquals(channel, channels.take());
        assertEquals(1, (long) counts.take());

        pubsub.quit();

        assertEquals(channel, channels.take());
        assertEquals(1, (long) counts.take());

        redis.publish(channel, message);
        assertEquals(channel, channels.take());
        assertEquals(message, messages.take());
    }

    @Test(timeout = 100)
    public void adapter() throws Exception {
        final BlockingQueue<String> localSubscriptions = new LinkedBlockingQueue<String>();
        RedisPubSubAdapter<String> adapter = new RedisPubSubAdapter<String>() {
            @Override
            public void subscribed(String channel, long count) {
                localSubscriptions.add(channel);
            }
        };
        pubsub.addListener(adapter);
        pubsub.subscribe(channel);
        assertEquals(channel, localSubscriptions.take());
    }

    // RedisPubSubListener implementation

    @Override
    public void message(String channel, String message) {
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        patterns.add(pattern);
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void subscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }

    @Override
    public void unsubscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }
}
