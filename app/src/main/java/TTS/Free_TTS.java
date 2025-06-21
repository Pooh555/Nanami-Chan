// package TTS;

// import com.sun.speech.freetts.Voice;
// import com.sun.speech.freetts.VoiceManager;

// /**
//  * This class demonstrates how to use FreeTTS to convert a string into audio
//  * and play it immediately.
//  *
//  * To run this code:
//  * 1. Ensure you have FreeTTS libraries (freetts.jar, cmu_us_kal.jar, cmudict04.jar, cmulex.jar, lpc10.jar)
//  * added to your project's classpath.
//  * If using Maven, add the freetts dependency to your pom.xml.
//  * 2. Run the main method.
//  */
// public class Free_TTS {

//     public static void ma() {
//         // Set the path to the FreeTTS data directory if needed.
//         // For some systems, this might be necessary if voices aren't found automatically.
//         // System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");

//         // The text to be spoken
//         String textToSpeak = "Hello, this is a demonstration of text to speech in Java using FreeTTS.";

//         // Attempt to get a voice. Common voices include "kevin" or "mbrola_us1".
//         // "kevin" is usually available by default with the cmu_us_kal voice pack.
//         Voice voice = null;
//         try {
//             System.out.println("Available voices: ");
//             VoiceManager voiceManager = VoiceManager.getInstance();
//             Voice[] voices = voiceManager.getVoices();
//             if (voices.length == 0) {
//                 System.err.println("No FreeTTS voices found. Please ensure FreeTTS libraries are correctly configured and in the classpath.");
//                 System.err.println("Refer to FreeTTS documentation for proper setup if you encounter issues.");
//                 return;
//             }

//             for (Voice v : voices) {
//                 System.out.println("  - " + v.getName());
//             }

//             // Try to allocate a specific voice. "kevin" is a common default.
//             voice = voiceManager.getVoice("kevin");

//             if (voice == null) {
//                 System.err.println("Voice 'kevin' not found. Trying the first available voice.");
//                 voice = voices[0]; // Fallback to the first available voice
//                 if (voice == null) {
//                     System.err.println("No voices could be allocated. Exiting.");
//                     return;
//                 }
//             }

//             // Allocate the voice
//             voice.allocate();

//             // Set properties for the voice (optional)
//             voice.setRate(150); // Speed of speech (words per minute)
//             voice.setPitch(100); // Pitch of the voice
//             voice.setVolume(0.9f); // Volume (0.0 to 1.0)

//             System.out.println("\nSpeaking the following text: \"" + textToSpeak + "\"");
//             // Speak the text
//             voice.speak(textToSpeak);
//             System.out.println("Finished speaking.");

//         } catch (Exception e) {
//             System.err.println("An error occurred during TTS: " + e.getMessage());
//             e.printStackTrace();
//         } finally {
//             // Deallocate the voice to release resources
//             if (voice != null) {
//                 voice.deallocate();
//             }
//         }
//     }
// }
