import js from '@eslint/js';
import globals from 'globals';

export default [
  { ignores: ['node_modules/**', 'dist/**', 'dist-e2e/**', 'coverage/**', 'test-results/**', 'playwright-report/**', 'e2e-results/**'] },
  {
    files: ['**/*.{js,jsx,mjs}'],
    ...js.configs.recommended,
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: { ecmaFeatures: { jsx: true } },
      globals: { ...globals.browser },
    },
    rules: {
      ...js.configs.recommended.rules,
      'no-restricted-syntax': ['error', {
        selector: 'JSXAttribute[name.name="dangerouslySetInnerHTML"]',
        message: 'Render text or React elements instead of inserting raw HTML.',
      }],
      'no-eval': 'error',
      'no-implied-eval': 'error',
      'no-new-func': 'error',
      'no-script-url': 'error',
      // Caught errors are often deliberately replaced by a safe UI message.
      'no-unused-vars': ['error', { args: 'after-used', argsIgnorePattern: '^_', caughtErrors: 'none', ignoreRestSiblings: true }],
    },
  },
  {
    files: ['*.config.js', 'eslint.config.js', 'e2e/**/*.{js,mjs}'],
    languageOptions: { globals: { ...globals.node } },
  },
  {
    files: ['src/**/*.test.{js,jsx}', 'src/test/**/*.{js,jsx}'],
    languageOptions: { globals: { ...globals.vitest } },
  },
];
