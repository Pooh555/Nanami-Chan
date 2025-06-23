package main;

import render.WindowManager;

public class Main {
    public static String personality = "Nanami Osaka";
    public static String modelName = "llama3:latest";

    public static void main(String[] args) throws Exception {
		WindowManager window = new WindowManager("Nanami Chan", 1600, 900);
		
		window.run();
    }
}
