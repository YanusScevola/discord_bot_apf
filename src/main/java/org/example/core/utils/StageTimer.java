package org.example.core.utils;import java.awt.Color;import java.util.List;import java.util.Timer;import java.util.TimerTask;import java.util.concurrent.Executors;import java.util.concurrent.ScheduledExecutorService;import java.util.concurrent.ScheduledFuture;import java.util.concurrent.TimeUnit;import net.dv8tion.jda.api.EmbedBuilder;import net.dv8tion.jda.api.entities.Message;import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;import net.dv8tion.jda.api.interactions.components.buttons.Button;public class StageTimer {    private Timer timer;    private TimerTask currentTask;    private long startTimeMillis;    private long delayTimeMilis;    private long pauseTimeMillis;    private boolean isPaused;    private Message message;    private String title = "";    private Runnable timerCallback;    private List<Button> buttons;    private final VoiceChannel tribuneVoiceChannel;    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);    private ScheduledFuture<?> delayFuture;    private ScheduledFuture<?> pauseFuture;    public StageTimer(VoiceChannel tribuneVoiceChannel) {        this.tribuneVoiceChannel = tribuneVoiceChannel;        this.timer = new Timer();        this.isPaused = false;    }//    private void startSilence() {//        player.playTrack(silentTrack.makeClone());  // Воспроизводим бесшумный трек//    }////    private void stopSilence() {//        player.stopTrack();  // Останавливаем воспроизведение//    }//    public void start(String title, long delayTimeSec, List<Button> buttons, Runnable timerCallback) {        this.timerCallback = timerCallback;        this.timer = new Timer();        this.title = title;        this.buttons = buttons;        this.startTimeMillis = System.currentTimeMillis();        this.delayTimeMilis = delayTimeSec * 1000L;        String timerText = "Таймер: <t:" + (startTimeMillis / 1000 + delayTimeMilis / 1000) + ":R>";        EmbedBuilder embedBuilder = new EmbedBuilder();        embedBuilder.setTitle(title).setDescription(timerText).setColor(Color.BLUE);        if (buttons != null && !buttons.isEmpty()) {            tribuneVoiceChannel.sendMessageEmbeds(embedBuilder.build()).setActionRow(buttons).queue(sentMessage -> {                this.message = sentMessage;                scheduleTimer(delayTimeSec * 1000L, title, buttons, timerCallback::run);            });        } else {            tribuneVoiceChannel.sendMessageEmbeds(embedBuilder.build()).queue(sentMessage -> {                this.message = sentMessage;                scheduleTimer(delayTimeSec * 1000L, title, buttons, timerCallback::run);            });        }    }    private void scheduleTimer(long delay, String title, List<Button> buttons, Runnable timerCallback) {        currentTask = new TimerTask() {            @Override            public void run() {                timerCallback.run();                if (buttons != null && !buttons.isEmpty()) {                    List<Button> disabledButtons = buttons.stream().map(Button::asDisabled).toList();                    EmbedBuilder updatedEmbed = new EmbedBuilder();                    updatedEmbed.setTitle(title).setDescription("Время вышло!").setColor(Color.BLUE);                    message.editMessageEmbeds(updatedEmbed.build()).setActionRow(disabledButtons).queue();                } else {                    EmbedBuilder updatedEmbed = new EmbedBuilder();                    updatedEmbed.setTitle(title).setDescription("Время вышло!").setColor(Color.BLUE);                    message.editMessageEmbeds(updatedEmbed.build()).queue();                }            }        };        timer.schedule(currentTask, delay);    }    public synchronized void pause(long timeSec, long delaySec, Runnable timerCallback) {        if (!isPaused && currentTask != null) {            currentTask.cancel();            isPaused = true;            long currentTimeMillis = System.currentTimeMillis();            long timeElapsedMillis = currentTimeMillis - startTimeMillis;            pauseTimeMillis = Math.max(0, delayTimeMilis - timeElapsedMillis);  // Защита от отрицательного времени            EmbedBuilder embedBuilder = new EmbedBuilder();            embedBuilder.setTitle("(На паузе) " + title).setColor(Color.YELLOW);            String staticTimerText = "Остановились на " + pauseTimeMillis / 1000 + " секунде.";            embedBuilder.setDescription(staticTimerText);            if (message != null) {                List<Button> disabledButtons = buttons.stream().map(Button::asDisabled).toList();                if (!buttons.isEmpty()) {                    message.editMessageEmbeds(embedBuilder.build()).setActionRow(disabledButtons).queue();                } else {                    message.editMessageEmbeds(embedBuilder.build()).queue();                }            }            delayFuture = scheduler.schedule(() -> {                embedBuilder.setColor(Color.YELLOW);                String timerText = "Вопрос: <t:" + (System.currentTimeMillis() / 1000 + timeSec) + ":R>";                embedBuilder.setDescription(staticTimerText + "\n" + timerText);                if (message != null) {                    message.editMessageEmbeds(embedBuilder.build()).queue();                }//                pauseFuture = scheduler.schedule(this::resume, timeSec, TimeUnit.SECONDS);                pauseFuture = scheduler.schedule(timerCallback::run, timeSec, TimeUnit.SECONDS);            }, delaySec, TimeUnit.SECONDS);        }    }    public synchronized void resume() {        if (isPaused) {            if (pauseFuture != null) {                pauseFuture.cancel(true);            }            if (delayFuture != null) {                delayFuture.cancel(true);            }            long currentTimeMillis = System.currentTimeMillis();            long resumeTimeSec = (currentTimeMillis + pauseTimeMillis) / 1000;            startTimeMillis = currentTimeMillis - (delayTimeMilis - pauseTimeMillis);            String timerText = "Таймер: <t:" + resumeTimeSec + ":R>";            EmbedBuilder embedBuilder = new EmbedBuilder();            embedBuilder.setTitle("" + title)                    .setDescription(timerText)                    .setColor(Color.BLUE);            if (message != null) {                if (buttons != null && !buttons.isEmpty()) {                    message.editMessageEmbeds(embedBuilder.build())                            .setActionRow(buttons)                            .queue(sentMessage -> {                                scheduleTimer(pauseTimeMillis, title, buttons, timerCallback);                            });                } else {                    message.editMessageEmbeds(embedBuilder.build())                            .queue(sentMessage -> {                                scheduleTimer(pauseTimeMillis, title, null, timerCallback);                            });                }            }            isPaused = false;        }    }    public synchronized void skip() {        if (currentTask != null && !isPaused) {            currentTask.cancel();            EmbedBuilder updatedEmbed = new EmbedBuilder();            updatedEmbed.setTitle(title).setDescription("Таймер был пропущен!").setColor(Color.BLUE);            if (message != null) {                List<Button> disabledButtons = buttons.stream().map(Button::asDisabled).toList();                if (!buttons.isEmpty()) {                    message.editMessageEmbeds(updatedEmbed.build()).setActionRow(disabledButtons).queue();                } else {                    message.editMessageEmbeds(updatedEmbed.build()).queue();                }            }            if (timerCallback != null) {                timerCallback.run();            }        }    }    public synchronized long getCurrentTimeLeft() {        if (isPaused) {            return pauseTimeMillis;        } else {            long currentTimeMillis = System.currentTimeMillis();            long timeElapsedMillis = currentTimeMillis - startTimeMillis;            return Math.max(0, delayTimeMilis - timeElapsedMillis);  // Защита от отрицательного времени        }    }    public String getTitle() {        return title;    }}