package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class ExprReplace extends SimpleExpression<String> {
	static {
		Skript.registerExpression(ExprReplace.class, String.class, ExpressionType.SIMPLE,
			"replace [(all|every)|first:[the] first] %strings% in %strings% with %string% [case:with case sensitivity]",
			"replace [(all|every)|first:[the] first] %strings% with %string% in %strings% [case:with case sensitivity]",
			"(replace [with|using] regex|regex replace) %strings% in %strings% with %string%",
			"(replace [with|using] regex|regex replace) %strings% with %string% in %strings%");
	}

	@Override
	public Class<? extends String> getReturnType() { return String.class; }

	public boolean isSingle() { return false; }

	private Expression<String> exprNeedle;
	private Expression<String> exprHaystack;
	private Expression<String> exprReplacement;

	private boolean isRegex = false;
	private boolean isFirst = false;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {

		if (matchedPattern == 0 || matchedPattern == 2) {
			exprNeedle = (Expression<String>) expr[0];
			exprHaystack = (Expression<String>) expr[1];
			exprReplacement = (Expression<String>) expr[2];
		} else {
			exprNeedle = (Expression<String>) expr[0];
			exprReplacement = (Expression<String>) expr[1];
			exprHaystack = (Expression<String>) expr[2];
		}

		isRegex = matchedPattern == 2 || matchedPattern == 3;
		isFirst = parseResult.hasTag("first");
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);

		builder.append("replace");

		if (isFirst) { builder.append("first"); }
		if (isRegex) { builder.append("regex"); }

		return builder.toString();
	}

	@Override
	@Nullable
	protected String[] get(Event event) {

		String replacement = exprReplacement.getSingle(event);

		if (replacement == null) {
			return null;
		}

		if (exprHaystack instanceof ExpressionList<String> list) {
			for (Expression<? extends String> haystackExpr : list.getExpressions()) {
				return (String[]) getReplacement(event, haystackExpr, replacement);
			}
		} else {
			return (String[]) getReplacement(event, exprHaystack, replacement);
		}
		return null;
	}

	@Nullable
	private Object[] getReplacement(Event event, Expression<? extends String> haystackExpr, String replacement) {
		String[] needles = exprNeedle.getAll(event);
		String[] haystacks = exprHaystack.getAll(event);

		ArrayList<String> result = new ArrayList<String>();

		for (String haystack : haystacks) {

			String haystackResult = haystack;

			for (String needle : needles) {
				if (isFirst) {
					haystackResult = haystackResult.replaceFirst(needle, replacement);
				} else {
					haystackResult = haystackResult.replaceAll(needle, replacement);
				}
			}

			result.add(haystackResult);

		}

		return result.toArray();

	}


}