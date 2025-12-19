require('dotenv').config();
const express = require('express');
const TelegramBot = require('node-telegram-bot-api');

const app = express();
app.use(express.json());

const BOT_TOKEN = process.env.BOT_TOKEN;
const PORT = process.env.PORT || 3000;

if (!BOT_TOKEN) {
    console.error('❌ BOT_TOKEN not set in .env');
    process.exit(1);
}

const bot = new TelegramBot(BOT_TOKEN, { polling: true });
const users = new Map();

bot.onText(/\/start/, (msg) => {
    const chatId = msg.chat.id;
    const userId = msg.from.id;
    users.set(userId.toString(), chatId);
    bot.sendMessage(chatId, 
        `🛡️ *Антимошенник*\n\nВы зарегистрированы!\nВаш ID: \`${userId}\`\n\nВведите этот ID в приложении.`,
        { parse_mode: 'Markdown' }
    );
});

bot.onText(/\/status/, (msg) => {
    bot.sendMessage(msg.chat.id, '🟢 Бот работает');
});

app.post('/api/alert', async (req, res) => {
    try {
        const { userId, score, text, timestamp } = req.body;
        if (!userId) return res.status(400).json({ error: 'userId required' });
        
        const chatId = users.get(userId.toString());
        if (!chatId) return res.status(404).json({ error: 'User not registered' });
        
        await bot.sendMessage(chatId, 
            `🚨 *ВНИМАНИЕ! МОШЕННИК!*\n\n📊 Баллы: *${score}*\n📝 Текст:\n_${text || 'Нет'}_\n\n⏰ ${timestamp}`,
            { parse_mode: 'Markdown' }
        );
        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

app.get('/health', (req, res) => res.json({ status: 'ok', users: users.size }));

app.listen(PORT, () => console.log(`🚀 Server on port ${PORT}`));
