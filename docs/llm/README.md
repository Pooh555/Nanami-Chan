<h1 align="left">Large Language Model Documentation</h1>

<p>This section describes the usage and development of large language model (LLM) implemented in this project.</p>

<h2 align="left">Information</h2>

<p>The LLM wrapper is located in 
    <a href="https://github.com/Pooh555/Nanami-Chan/tree/main/app/src/main/java/com/nanami/llm_wrapper" target="_blank">/app/src/main/java/com/nanami/llm_wrapper
  </a>
  .
  <br></br>
  This code handles the request and retrieval of data between the client and LLM's API. In addition to that, basic functionalities that allow a smooth user experience is also located here. Some fundamental functions includes:

  - Service initialization.
  - POST an input prompt to the server.
  - GET a response from the server.
  - Manage conversation history and model's memory.
  - Service termination</p>

<h2 align="left">Basic Usage</h2>

<p>Available LLM models:

 -  Ollama: llama3:lasest (default)
 
 Available personalities:
 
 - Kita Ikuyo (From ぼっちざろっく!)
 - Nanami Osaka (From 現実もたまには嘘をつく) (default)</p>

 You can customize your own model and its personaility by edit the constants in /app/src/main/java/com/nanami/MainActivity.java. 

<p>TODO: Write full documentation for this part</p>
