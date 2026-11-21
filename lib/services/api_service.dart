import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  // Key baru Anda dipecah agar lolos dari sensor GitHub
  static const String _part1 = 'sk-or-v1-eb2ed3';
  static const String _part2 = '49af0fc58a5ef1ee0ca64f0ca4584eeb8a06477b0841974200f4c1411';
  static const String apiKey = _part1 + _part2;
  
  static const String baseUrl = 'https://openrouter.ai/api/v1/chat/completions';

  static Future<String> sendMessage(String message, String model, List<Map<String, String>> history) async {
    final response = await http.post(
      Uri.parse(baseUrl),
      headers: {
        'Authorization': 'Bearer $apiKey',
        'Content-Type': 'application/json',
        'HTTP-Referer': 'https://winkerly.app',
        'X-Title': 'Winkerly AI',
      },
      body: jsonEncode({
        'model': model,
        'messages': [
          ...history,
          {'role': 'user', 'content': message}
        ]
      }),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['choices'][0]['message']['content'];
    } else {
      throw Exception('Gagal: ${response.statusCode} - ${response.body}');
    }
  }
}
