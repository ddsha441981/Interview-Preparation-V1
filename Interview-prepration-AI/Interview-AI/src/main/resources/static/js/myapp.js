document.addEventListener('DOMContentLoaded', () => {
    const startBtn = document.getElementById('start-btn');
    const stopBtn = document.getElementById('stop-btn');
    const transcription = document.getElementById('transcription');
    const chatgptResponse = document.getElementById('chatgpt-response')?.querySelector('.response-content');
    const copilotResponse = document.getElementById('copilot-response')?.querySelector('.response-content');
    const geminiResponse = document.getElementById('gemini-response')?.querySelector('.response-content');
    let recognition;

    if (!transcription || !chatgptResponse || !copilotResponse || !geminiResponse) {
        console.error('One or more elements are not found in the DOM.');
        return;
    }

    if ('webkitSpeechRecognition' in window) {
        recognition = new webkitSpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = 'en-US';

        let finalTranscript = '';
        let lastProcessedTranscript = '';
        let processDelay = 5000; // Delay 2 sec

        // NLP Algorithm to capture and detect preveious speak data of interviewer
        const levenshteinDistance = (a, b) => {
            const matrix = [];

            for (let i = 0; i <= b.length; i++) {
                matrix[i] = [i];
            }

            for (let j = 0; j <= a.length; j++) {
                matrix[0][j] = j;
            }

            for (let i = 1; i <= b.length; i++) {
                for (let j = 1; j <= a.length; j++) {
                    if (b.charAt(i - 1) === a.charAt(j - 1)) {
                        matrix[i][j] = matrix[i - 1][j - 1];
                    } else {
                        matrix[i][j] = Math.min(matrix[i - 1][j - 1] + 1, Math.min(matrix[i][j - 1] + 1, matrix[i - 1][j] + 1));
                    }
                }
            }

            return matrix[b.length][a.length];
        };

        const areSimilar = (a, b) => {
            const distance = levenshteinDistance(a, b);
            const similarity = 1 - (distance / Math.max(a.length, b.length));
            return similarity > 0.8; // Adjust threshold as necessary
        };

        //Process transccripts
        const processTranscript = (transcript) => {
            if (!areSimilar(lastProcessedTranscript, transcript)) {
                lastProcessedTranscript = transcript;

                fetch('http://localhost:7070/api/v1/interview/ai/transcribe', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ transcript })
                })
                    .then(response => response.json())
                    .then(data => {
                        console.log('Parsed Data:', data);

                        if (chatgptResponse && copilotResponse && geminiResponse) {
                            if (data.chatgpt) {
                                chatgptResponse.innerHTML += `<div class="response-block">${data.chatgpt}</div>`;
                            } else {
                                chatgptResponse.innerHTML += '<div class="response-block">No data available</div>';
                            }

                            if (data.copilot) {
                                copilotResponse.innerHTML += `<div class="response-block">${data.copilot}</div>`;
                            } else {
                                copilotResponse.innerHTML += '<div class="response-block">No data available</div>';
                            }

                            if (data.gemini) {
                                try {
                                    const geminiData = JSON.parse(data.gemini);
                                    const geminiContent = geminiData.candidates.map(candidate => {
                                        if (candidate.content && candidate.content.parts && candidate.content.parts.length > 0) {
                                            return candidate.content.parts.map(part => {
                                                if (part.text) {
                                                    let formattedText = part.text
                                                        .replace(/^#\s+(.*)$/gm, '<h1>$1</h1>')
                                                        .replace(/^##\s+(.*)$/gm, '<h2>$1</h2>')
                                                        .replace(/^###\s+(.*)$/gm, '<h3>$1</h3>')
                                                        .replace(/^\*\s+(.*)$/gm, '<ul><li>$1</li></ul>')
                                                        .replace(/^\d+\.\s+(.*)$/gm, '<ol><li>$1</li></ol>')
                                                        .replace(/```([\s\S]*?)```/g, '<div class="code-editor-wrapper"><textarea class="code-editor">$1</textarea></div>')
                                                        .replace(/\*\*(.*?)\*\*/g, '<span class="bold">$1</span>')
                                                        .replace(/\*(.*?)\*/g, '<span class="italic">$1</span>')
                                                        .replace(/\n\*\s+(.*)/g, '<br><span class="italic">$1</span>');
                                                    return `<div class="response-block">${formattedText}</div>`;
                                                }
                                                return '<div class="response-block">No content available</div>';
                                            }).join('');
                                        } else {
                                            return '<div class="response-block">No content available</div>';
                                        }
                                    }).join('');

                                    geminiResponse.innerHTML += geminiContent;

                                    // Initialize CodeMirror
                                    document.querySelectorAll('.code-editor').forEach((el) => {
                                        const editorWrapper = el.closest('.code-editor-wrapper');
                                        if (editorWrapper) {
                                            editorWrapper.style.display = 'block';
                                            CodeMirror.fromTextArea(el, {
                                                lineNumbers: true,
                                                mode: "javascript",
                                                theme: 'monokai',
                                                autoCloseBrackets: true,
                                                matchBrackets: true
                                            });
                                        }
                                    });

                                } catch (e) {
                                    geminiResponse.innerHTML += `<div class="response-block">Error parsing Google Gemini data</div>`;
                                    console.error('Error parsing Google Gemini data:', e);
                                }
                            } else {
                                geminiResponse.innerHTML += '<div class="response-block">No data available</div>';
                            }
                        }
                    })
                    .catch(error => {
                        console.error('Fetch Error:', error);
                        if (chatgptResponse) chatgptResponse.innerHTML += `<div class="response-block"><strong>Error:</strong> ${error.message}</div>`;
                    });
            }
        };

        recognition.onresult = function (event) {
            let result = '';
            for (let i = event.resultIndex; i < event.results.length; i++) {
                result += event.results[i][0].transcript;
            }
            transcription.textContent = result;
            finalTranscript = result;

            // Throttle the processing to reduce delay
            setTimeout(() => {
                processTranscript(finalTranscript);
            }, processDelay);
        };

        recognition.onerror = function (event) {
            console.error('Speech recognition error detected:', event.error);
        };

        recognition.onend = function () {
            console.log('Speech recognition service disconnected');
            processTranscript(finalTranscript);
        };

        startBtn.addEventListener('click', () => {
            recognition.start();
        });

        stopBtn.addEventListener('click', () => {
            recognition.stop();
        });
    } else {
        transcription.textContent = 'Speech Recognition not supported in this browser.';
    }
});