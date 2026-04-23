package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
	public boolean canBeSingle() { return true; }

	private Expression<String> exprNeedle;
	private Expression<String> exprHaystack;
	private Expression<String> exprReplacement;

	private boolean isRegex;
	private boolean isFirst;
	private boolean isCaseSensitive = false;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {

		if (matchedPattern == 0 || matchedPattern == 2) {
			exprHaystack = (Expression<String>) expr[1];
			exprReplacement = (Expression<String>) expr[2];
		} else {
			exprHaystack = (Expression<String>) expr[2];
			exprReplacement = (Expression<String>) expr[1];
		}

		exprNeedle = (Expression<String>) expr[0];

		isRegex = matchedPattern == 2 || matchedPattern == 3;
		isFirst = parseResult.hasTag("first");

		if (SkriptConfig.caseSensitive.value() || parseResult.hasTag("case")) {
			isCaseSensitive = true;
		}
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);

		builder.append("replace");

		if (isFirst) { builder.append("first"); }
		if (isRegex) { builder.append("regex"); }
		if (isCaseSensitive) { builder.append("case sensitive"); }

		return builder.toString();
	}

	@Override
	@Nullable
	protected String[] get(Event event) {
		String replacement = exprReplacement.getSingle(event);
		String[] needles = exprNeedle.getAll(event);
		String[] haystacks = exprHaystack.getAll(event);

		if (replacement == null) {
			return haystacks;
		}

		List<String> result = new ArrayList<>();

		for (String haystack : haystacks) {
			for (String needle : needles) {
				if (isRegex) {
					try {
						if (isFirst) {
							haystack = haystack.replaceFirst(needle, replacement);
						} else {
							haystack = haystack.replaceAll(needle, replacement);
						}
					} catch (Exception ignored) {}

				} else {

					if (isFirst) {
						haystack = StringUtils.replaceFirst(haystack, needle, replacement, isCaseSensitive);
					} else {
						haystack = StringUtils.replace(haystack, needle, replacement, isCaseSensitive);
					}
				}

			}
			result.add(haystack);
		}

		return result.toArray(new String[0]);

	}
}