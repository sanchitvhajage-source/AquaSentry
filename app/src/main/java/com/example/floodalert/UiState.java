//
// --- FINAL FIX: Package now matches the file's location ---
//
package com.example.floodalert;

public abstract class UiState {

    private UiState() {}

    public static final class Loading extends UiState {
        public final String message;
        public Loading(String message) { this.message = message; }
    }

    public static final class Success<T> extends UiState {
        public final T data;
        public Success(T data) { this.data = data; }
    }

    public static final class Error extends UiState {
        public final String message;
        public Error(String message) { this.message = message; }
    }
}