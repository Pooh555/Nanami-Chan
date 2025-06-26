<h1 align="center">Nanami Chan</h1>

<div align="center">
  <img src="res/banner/sometimes_even_reality_is_a_lie_banner_4.jpeg" style="width: 45%; display: inline-block;" />
  <img src="res/banner/sometimes_even_reality_is_a_lie_banner_5.jpeg" style="width: 45%; display: inline-block;" />
</div>

<h2 align="left">Introduction</h2>

<p>With the advent of large language model (LLM), brining your waifu to life has never been more plausible. In this project, I created an Nanami Chan (She is form the manga "Sometimes Even Reality Is a Lie."), who can comfort you whenever you need her.</p> 

> Inspired by [Neuro-sama](https://www.youtube.com/@Neurosama)

<p>If you have ever watched neuro Neuro-sama, a popular AI-Vtuber, that is a wonderful way to grasp this project as this work is an attempt to replicate Neuro-sama, which is a closed-source project, using Java.</p>

> Inspired by [Project-Airi](https://github.com/moeru-ai/airi)

<h2 align="left">Advantages</h2>

<p>Because this project's backend is mainly written in Java, it can be run on any platform with Java Virtual Machine (JVM). Unlike other simaillar projects, the models and services behind this work are also free to use without any paywall.</p>


<h2 align="left">Capabilities</h2>

- [x] Brain
  - [x] Chat in terminal
  - [ ] Chat in [Discord](https://discord.com)
  - [ ] Memory
- [x] Ears
  - [x] Audio input from terminal using Vosk
  - [ ] Audio input from [Discord](https://discord.com)
- [x] Mouth
  - [x] [ElevenLabs](https://elevenlabs.io/) voice synthesis
- [x] Body
  - [x] Live2D support
    - [ ] Control Live2D model
  - [x] Live2D model animations
    - [x] Auto blink
    - [ ] Auto look at
    - [ ] Idle eye movement

<h2 align="left">Requirements</h2>

- OpenJDK: 24 

<h2 align="left">Developemnt</h2>
<p>First step (crucial step), copy dummy_files/API_keys.java to app/java/com/nanami/keys
  <br></br>
  You can do it manually or use the script below
</p>

```
chmod +x build.sh
./build.sh
```

<p>If you are building the project for the first time, I recommend you to use Android studio for convenience. Otherwise you can install the app using the terminal via the command below.</p>

```
./gradlew installDebug
```
