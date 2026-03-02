// AI service enabled (previously disabled for review)
/**
 * AI Service for Journal App
 * Provides AI-powered rewrite functionality
 * 
 * NOTE: For production, you should proxy OpenAI requests through your backend
 * to keep API keys secure. For development, you can use this directly.
 */

const OPENAI_API_KEY = process.env.REACT_APP_OPENAI_API_KEY || '';
// Use backend proxy by default for security (recommended for production)
// Set REACT_APP_USE_AI_PROXY=false to use direct OpenAI calls (development only)
const USE_BACKEND_PROXY = process.env.REACT_APP_USE_AI_PROXY !== 'false';

/**
 * Rewrite title and content with AI for better grammar and clarity
 * @param {string} title - Journal entry title
 * @param {string} content - Journal entry content
 * @returns {Promise<{title: string, content: string}>} Rewritten title and content
 */
export const rewriteWithAI = async (title, content) => {
  if (!title || !title.trim() || !content || content.trim().length < 10) {
    return { title: title || '', content: content || '' };
  }

  try {
    if (USE_BACKEND_PROXY) {
      const { aiAPI } = await import('./api');
      const response = await aiAPI.rewrite(title, content);
      return response.data || { title, content };
    } else {
      // Direct OpenAI API call (for development only)
      const prompt = `Rewrite the following journal entry title and content to be more grammatically correct, meaningful, and well-written. ` +
        `Keep the same meaning and tone, but improve clarity, grammar, and flow. ` +
        `Return a JSON object with exactly two keys: "title" (the improved title) and "content" (the improved content). ` +
        `Return ONLY valid JSON, no other text.\n\n` +
        `Original Title: ${title}\n\n` +
        `Original Content: ${content.substring(0, 1500)}`;

      const response = await fetch('https://api.deepseek.com/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${OPENAI_API_KEY}`,
        },
        body: JSON.stringify({
          model: 'deepseek-chat',
          messages: [
            {
              role: 'system',
              content: 'You are a professional writing assistant that improves journal entries while preserving the author\'s voice and meaning. Always return valid JSON only, no other text.',
            },
            {
              role: 'user',
              content: prompt,
            },
          ],
          max_tokens: 800,
          temperature: 0.5,
        }),
      });

      if (!response.ok) {
        throw new Error('Failed to rewrite with AI');
      }

      const data = await response.json();
      const result = JSON.parse(data.choices[0]?.message?.content || '{}');
      return {
        title: result.title || title,
        content: result.content || content,
      };
    }
  } catch (error) {
    console.error('[AI Service] Error rewriting with AI:', error);
    return { title, content }; // Return original on error
  }
};