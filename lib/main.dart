import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:google_fonts/google_fonts.dart';
import 'theme.dart';
import 'services/api_service.dart';

void main() {
  runApp(const WinkerlyApp());
}

class WinkerlyApp extends StatelessWidget {
  const WinkerlyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Winkerly',
      theme: winkerlyDark.copyWith(
        textTheme: GoogleFonts.interTextTheme(winkerlyDark.textTheme),
      ),
      debugShowCheckedModeBanner: false,
      home: const ChatScreen(),
    );
  }
}

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final List<Map<String, String>> _messages = [];
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  bool _loading = false;
  
  // DAFTAR MODEL 100% GRATIS
  String _selectedModel = 'meta-llama/llama-3-8b-instruct:free';

  final List<Map<String, String>> _models = [
    {'id': 'meta-llama/llama-3-8b-instruct:free', 'name': 'Llama 3 8B (Free)'},
    {'id': 'google/gemma-2-9b-it:free', 'name': 'Gemma 2 9B (Free)'},
    {'id': 'mistralai/mistral-7b-instruct:free', 'name': 'Mistral 7B (Free)'},
    {'id': 'openai/gpt-4o-mini', 'name': 'GPT-4o Mini (Butuh Saldo)'},
    {'id': 'anthropic/claude-3.5-sonnet', 'name': 'Claude 3.5 (Butuh Saldo)'},
  ];

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    final prefs = await SharedPreferences.getInstance();
    final savedHistory = prefs.getString('chat_history');
    if (savedHistory != null) {
      setState(() {
        _messages.addAll(List<Map<String, String>>.from(jsonDecode(savedHistory)));
      });
      _scrollToBottom();
    }
  }

  Future<void> _saveHistory() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('chat_history', jsonEncode(_messages));
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Future<void> _send() async {
    final text = _controller.text.trim();
    if (text.isEmpty || _loading) return;

    setState(() {
      _messages.add({'role': 'user', 'content': text});
      _loading = true;
      _controller.clear();
    });
    _scrollToBottom();

    try {
      final history = _messages.sublist(0, _messages.length - 1);
      final reply = await ApiService.sendMessage(text, _selectedModel, history);
      
      setState(() {
        _messages.add({'role': 'assistant', 'content': reply});
      });
      _saveHistory();
    } catch (e) {
      setState(() {
        // Menampilkan error detail jika masih 401
        _messages.add({'role': 'assistant', 'content': e.toString()});
      });
    } finally {
      setState(() => _loading = false);
      _scrollToBottom();
    }
  }

  void _clearChat() {
    setState(() {
      _messages.clear();
    });
    _saveHistory();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_models.firstWhere((m) => m['id'] == _selectedModel)['name']!),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
            onPressed: _messages.isEmpty ? null : _clearChat,
          ),
        ],
      ),
      drawer: Drawer(
        backgroundColor: const Color(0xFF1A1A1A),
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            DrawerHeader(
              decoration: const BoxDecoration(color: Color(0xFF0A0A0A)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Image.asset('assets/logo.png', height: 50, width: 50),
                  const SizedBox(height: 10),
                  const Text('Winkerly', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white)),
                  const Text('AI All-Rounder', style: TextStyle(color: Colors.white54)),
                ],
              ),
            ),
            const Padding(
              padding: EdgeInsets.all(16.0),
              child: Text('Pilih Model AI', style: TextStyle(color: Color(0xFF6C63FF), fontWeight: FontWeight.bold)),
            ),
            ..._models.map((m) => ListTile(
              leading: Icon(Icons.smart_toy, color: m['id'] == _selectedModel ? const Color(0xFF6C63FF) : Colors.white54),
              title: Text(m['name']!, style: TextStyle(color: m['id'] == _selectedModel ? Colors.white : Colors.white54)),
              onTap: () {
                setState(() => _selectedModel = m['id']!);
                Navigator.pop(context);
              },
            )),
          ],
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: _messages.isEmpty
                ? const Center(child: Text('Mulai obrolan baru...', style: TextStyle(color: Colors.white54)))
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(12),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      final msg = _messages[index];
                      final isUser = msg['role'] == 'user';
                      return Align(
                        alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                        child: GestureDetector(
                          onLongPress: () {
                            Clipboard.setData(ClipboardData(text: msg['content']!));
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Teks disalin!'), duration: Duration(seconds: 1)),
                            );
                          },
                          child: Container(
                            margin: const EdgeInsets.symmetric(vertical: 4),
                            padding: const EdgeInsets.all(14),
                            constraints: BoxConstraints(
                              maxWidth: MediaQuery.of(context).size.width * 0.85,
                            ),
                            decoration: BoxDecoration(
                              color: isUser ? const Color(0xFF6C63FF) : const Color(0xFF1E1E1E),
                              borderRadius: BorderRadius.circular(16).copyWith(
                                bottomRight: isUser ? const Radius.circular(0) : const Radius.circular(16),
                                bottomLeft: !isUser ? const Radius.circular(0) : const Radius.circular(16),
                              ),
                            ),
                            child: MarkdownBody(
                              data: msg['content']!,
                              styleSheet: MarkdownStyleSheet(
                                p: TextStyle(color: isUser ? Colors.white : Colors.white70, fontSize: 15),
                                code: const TextStyle(backgroundColor: Colors.black45, color: Colors.greenAccent),
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
          ),
          if (_loading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                  SizedBox(width: 10),
                  Text('Winkerly sedang berpikir...', style: TextStyle(color: Colors.white54, fontSize: 12)),
                ],
              ),
            ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      decoration: InputDecoration(
                        hintText: 'Tanya apa saja...',
                        contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                        filled: true,
                        fillColor: const Color(0xFF1A1A1A),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(30),
                          borderSide: BorderSide.none,
                        ),
                      ),
                      onSubmitted: (_) => _send(),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    decoration: const BoxDecoration(
                      color: Color(0xFF6C63FF),
                      shape: BoxShape.circle,
                    ),
                    child: IconButton(
                      icon: const Icon(Icons.send, color: Colors.white),
                      onPressed: _send,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
