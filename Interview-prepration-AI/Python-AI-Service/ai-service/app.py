from flask import Flask, request, jsonify
import Levenshtein
from werkzeug.middleware.proxy_fix import ProxyFix

app = Flask(__name__)

@app.route('/levenshtein', methods=['POST'])
def calculate_levenshtein():
    str1 = request.form.get('str1', '')
    str2 = request.form.get('str2', '')
    distance = Levenshtein.distance(str1, str2)
    
    return str(distance)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
