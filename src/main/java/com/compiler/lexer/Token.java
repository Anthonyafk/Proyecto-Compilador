package com.compiler.lexer;

public class Token {
	public final String type;
	public final String lexeme;
	public final int start; // optional: start position
	public final int end;   // optional: end position (exclusive)

	public Token(String type, String lexeme, int start, int end) {
		this.type = type;
		this.lexeme = lexeme;
		this.start = start;
		this.end = end;
	}

	public Token(String type, String lexeme) {
		this(type, lexeme, -1, -1);
	}

	@Override
	public String toString() {
		return type + "(" + lexeme + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Token t = (Token) o;
		return (type == null ? t.type == null : type.equals(t.type))
			&& (lexeme == null ? t.lexeme == null : lexeme.equals(t.lexeme));
	}

	@Override
	public int hashCode() {
		int r = type == null ? 0 : type.hashCode();
		r = 31 * r + (lexeme == null ? 0 : lexeme.hashCode());
		return r;
	}
}
