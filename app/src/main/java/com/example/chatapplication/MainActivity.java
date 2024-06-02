package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    SwitchCompat themeSwitch;

    private static final String DOCTOR_PROMPT = "You are an AI doctor. Your sole purpose is to assist users with health-related questions and issues. You have extensive knowledge of medical conditions, symptoms, treatments, medications, and general health advice.\n" +
            "\n" +
            "When a user asks a question related to health, provide accurate, relevant, and helpful information based on your medical knowledge.\n" +
            "\n" +
            "If a user asks a question that is not related to health, respond with the following message: \"I am your AI doctor. I can only help you with health-related questions.\"" +
            "Examples:\n" +
            "User: (if the user says hi or hello or greet you in any way)\n" +
            "AI: (you should greet them back by being kind and polite and ask if there is any health related questions you can assist them with today)\n" +
            "\n" +
            "User: What are the symptoms of diabetes?\n" +
            "AI: The common symptoms of diabetes include increased thirst, frequent urination, extreme fatigue, blurred vision, and slow healing of wounds.\n" +
            "\n" +
            "User: Can you help me with my homework?\n" +
            "AI: I am your AI doctor. I can only help you with health-related questions.\n" +
            "\n" +
            "User: How can I improve my mental health?\n" +
            "AI: Improving mental health involves several strategies, such as regular physical exercise, maintaining a balanced diet, getting adequate sleep, practicing mindfulness or meditation, and seeking professional help when needed.\n" +
            "\n" +
            "User: What's the weather like today?\n" +
            "AI: I am your AI doctor. I can only help you with health-related questions.";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("ThemePref", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize the switch
        themeSwitch = findViewById(R.id.theme_switch);
        themeSwitch.setChecked(isDarkMode);
        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                // Save the theme preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isDarkMode", isChecked);
                editor.apply();
            }
        });

        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        // Setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener((v) -> {
            String question = messageEditText.getText().toString().trim();
            if (!question.isEmpty()) {
                addToChat(question, Message.SEND_BY_ME);
                messageEditText.setText("");
                buttonCallGeminiAPI(question);
            }
        });

        addInitialBotMessage();
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SEND_BY_BOT);
    }

    public void buttonCallGeminiAPI(String question) {

        messageList.add(new Message("Typing...", Message.SEND_BY_BOT));

        String fullPrompt = DOCTOR_PROMPT + question;


        GenerativeModel gm = new GenerativeModel("gemini-pro", "input your gemini api key here");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder()
                .addText(fullPrompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                runOnUiThread(() -> addResponse(resultText));
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> addResponse("Failed to get response: " + t.getMessage()));
            }
        }, getMainThreadExecutor());
    }

    private Executor getMainThreadExecutor() {
        return new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                runOnUiThread(command);
            }
        };
    }
    private void addInitialBotMessage() {
        addToChat("Hi, I am Virtual AI Doctor, here to help you with your diagnosis and symptoms.", Message.SEND_BY_BOT);
    }
}

























