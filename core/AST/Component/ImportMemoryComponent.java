package core.AST.Component;

import core.AST.Type.MemoryType;
import core.Parser;
import core.util.ParseException;
import core.util.Result.Err;
import core.util.Result.Ok;
import core.util.Result.Result;

public final class ImportMemoryComponent implements ImportComponentBase {
    private final String mod, name;
    MemoryType mt;

    ImportMemoryComponent(String mod, String name, MemoryType mt) {
        this.mod = mod;
        this.name = name;
        this.mt = mt;
    }

    public static Result<ImportComponentBase, ParseException> parseComponent(String mod, String name, Parser parser) {
        return switch (MemoryType.parse(parser)) {
            case Err(ParseException e) -> new Err<>(e);
            case Ok(MemoryType mt) -> new Ok<>(new ImportMemoryComponent(mod, name, mt));
        };
    }
}
