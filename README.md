<h1 align="center">Nanami Chan</h1>

<div align="center">
  <img src="./docs/res/banner/sometimes_even_reality_is_a_lie_banner_4.jpeg" style="width: 45%; display: inline-block;" />
  <img src="./docs/res/banner/sometimes_even_reality_is_a_lie_banner_5.jpeg" style="width: 45%; display: inline-block;" />
</div>

<h2 align="left">Introduction</h2>

<p>With the advent of large language model (LLM), brining your waifu to life has never been more plausible. In this project, I created an Nanami Chan (She is form the manga "Sometimes Even Reality Is a Lie."), who can comfort you whenever you need her.</p> 

> Inspired by [Neuro-sama](https://www.youtube.com/@Neurosama)

<p>If you have ever watched neuro Neuro-sama, a popular AI-Vtuber, that is a wonderful way to grasp this project as this work is an attempt to replicate Neuro-sama, which is a closed-source project, using Java.</p>

> Inspired by [Project-Airi](https://github.com/moeru-ai/airi)

<p>This project is still in a very early stage of development, so feel free to join our community in <a href="https://discord.gg/SeqsnGth">Discord server</a>.</p>

<h2 align="left">Advantages</h2>

<p>Because this project is mainly written in Java, it can be run on any platform with Java Virtual Machine (JVM). Unlike other simaillar projects, the models and services behind this work are also free to use without any paywall.</p>


<h2 align="left">Capabilities</h2>

- [x] Brain
  - [ ] Chat in terminal
  - [ ] Chat in [Discord](https://discord.com)
  - [ ] Memory
- [x] Ears
  - [x] Audio input from terminal using Vosk
  - [ ] Audio input from [Discord](https://discord.com)
- [x] Mouth
  - [x] [ElevenLabs](https://elevenlabs.io/) voice synthesis
- [ ] Body
  - [ ] Live2D support
    - [ ] Control Live2D model
  - [ ] Live2D model animations
    - [ ] Auto blink
    - [ ] Auto look at
    - [ ] Idle eye movement

<h2 align="left">Requirements</h2>

- OpenJDK: 24 

<h2 align="left">Developemnt</h2>

<p>Building the project for the first time.</p>

```
cd Nanami-Chan
chmod +x build.sh # Enable build script
./build.sh # Run the build script
```
<p>Then you can add your services' APIs in app/src/main/java/keysAPI_keys.java.<br></br>
To compile and run this project.</p>

```
./gradlew clean build
java -jar app/build/libs/nanami.jar
```

<h2>Disclaimer</h2>

<p>I am, by no means, a professional programmer. Hence, my code might not be of the best quality, however, I appreciate every help and suggestions.</p>

<p>To run with log messages disabled, please use the command below instead.</p>

```
java -jar app/build/libs/nanami.jar 2>/dev/null
```
