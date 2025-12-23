import { defineConfig, globalIgnores } from "eslint/config"
import nextVitals from "eslint-config-next/core-web-vitals"
import nextTs from "eslint-config-next/typescript"
import js from '@eslint/js'
import prettier from 'eslint-config-prettier'
import importPlugin from 'eslint-plugin-import'
import unusedImports from 'eslint-plugin-unused-imports'
import globals from 'globals'
import tseslint from 'typescript-eslint'

const eslintConfig = defineConfig([
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...nextVitals,
  ...nextTs,
  prettier,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
  {
    files: ['**/*.{ts,tsx,js,jsx}'],
    rules: {
      'react-hooks/set-state-in-effect': 'off',
    },
  },

  {
    files: ['**/*.{ts,tsx}'],
    plugins: {
      '@typescript-eslint': tseslint.plugin,
      import: importPlugin,
      'unused-imports': unusedImports,
    },
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        project: ['./tsconfig.json'],
        ecmaVersion: 'latest',
        sourceType: 'module',
      },
      globals: globals.browser,
    },
    rules: {
      'no-console': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn'],
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-multiple-empty-lines': ['warn', { max: 1, maxEOF: 0 }],
      'no-var': 'error',
      'prefer-const': 'warn',
      'import/no-duplicates': 'error',
      'unused-imports/no-unused-imports': 'error',
      'no-unused-vars': 'off',
    },
  },
]);

export default eslintConfig;
