import React, {
    createContext,
    Dispatch,
    PropsWithChildren,
    SetStateAction,
    useCallback,
    useContext,
    useEffect,
    useLayoutEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import { __, CurriedFunction1, CurriedFunction2, curry, isArray, pickBy } from "lodash";
import { useDebounce, useDebouncedValue } from "rooks";
import { useSearchParams } from "react-router-dom";

function serializeToQuery<T>(filterModel: T): [string, string][] {
    return Object.entries(filterModel)
        .flatMap(([key, value]) => (isArray(value) ? value.map((v: string) => ({ key, value: v })) : { key, value }))
        .map(({ key, value }) => [key, value]);
}

function deserializeFromQuery<T extends Record<Uppercase<string>, any>>(params: URLSearchParams): T {
    return [...params].reduce((result, [key, _value]) => {
        const value = _value === "true" || _value;
        return {
            ...result,
            [key]: result[key] && result[key] !== value ? [].concat(result[key]).concat(value) : value,
        };
    }, {} as any);
}

function ensureArray<T>(value: T | T[]): T[] {
    return value ? [].concat(value) : [];
}

type EnsureArray<V> = V extends Array<any> ? V : V[];

interface GetFilter<M> {
    <I extends keyof M, V extends M[I]>(id: I, ensureArray: true): EnsureArray<V>;

    <I extends keyof M, V extends M[I]>(id: I, ensureArray?: false): V;
}

interface FilterSetter<M, R = void> {
    <I extends keyof M, V extends M[I]>(id: I, value: V): R;
}

interface SetFilter<M> extends FilterSetter<M> {
    <I extends keyof M, V extends M[I]>(): CurriedFunction2<I, V, void>;

    <I extends keyof M, V extends M[I]>(id: I): CurriedFunction1<V, void>;

    <I extends keyof M, V extends M[I]>(id: __, value: V): CurriedFunction1<I, void>;
}

interface FiltersModelContextType<S = any> {
    model: S;
    setModel: Dispatch<SetStateAction<S>>;
}

export interface FiltersContextType<M = any> {
    getFilter: GetFilter<M>;
    setFilter: SetFilter<M>;
    setFilterImmediately: SetFilter<M>;
    activeKeys: Array<keyof M>;
}

export interface ValueLinker<M = any> {
    (setNewValue: FilterSetter<M, (prev: M) => M>): FilterSetter<M, (prev: M) => M>;
}

const FiltersModelContext = createContext<FiltersModelContextType>(null);
const ValueLinkerContext = createContext<ValueLinker>(null);

export function useFilterContext<M = unknown>(): FiltersContextType<M> {
    const { setModel, model } = useContext<FiltersModelContextType<M>>(FiltersModelContext);
    const [debouncedModel, setModelImmediately] = useDebouncedValue(model, 200);

    const getValueLinker = useContext<ValueLinker<M>>(ValueLinkerContext);

    const getValueSetter = useMemo<FilterSetter<M, (prev: M) => M>>(() => {
        return (id, value) => (current) =>
            pickBy(
                {
                    ...current,
                    [id]: value,
                },
                (value) => (isArray(value) ? value.length : !!value),
            ) as unknown as M;
    }, []);

    const getValueSetterWithLinker = useMemo<FilterSetter<M, (prev: M) => M>>(() => {
        return (id, value) => {
            const setter = getValueSetter(id, value);
            const linker = getValueLinker?.(getValueSetter);
            const withLinked = linker?.(id, value);
            return withLinked ? (current) => withLinked(setter(current)) : setter;
        };
    }, [getValueSetter, getValueLinker]);

    const setFilter = useCallback<FilterSetter<M>>(
        (id, value) => {
            const setter = getValueSetterWithLinker(id, value);
            setModel(setter);
        },
        [getValueSetterWithLinker, setModel],
    );

    const setFilterImmediately = useCallback<FilterSetter<M>>(
        (id, value) => {
            const setter = getValueSetterWithLinker(id, value);
            setModelImmediately(setter);
            setModel(setter);
        },
        [getValueSetterWithLinker, setModel, setModelImmediately],
    );

    const getFilter = useCallback<GetFilter<M>>(
        (field, forceArray) => {
            const value = debouncedModel[field];
            return forceArray ? ensureArray(value) : value;
        },
        [debouncedModel],
    );

    return useMemo<FiltersContextType<M>>(
        () => ({
            getFilter,
            setFilter: curry(setFilter),
            setFilterImmediately: curry(setFilterImmediately),
            activeKeys: Object.keys(debouncedModel || {}) as Array<keyof M>,
        }),
        [getFilter, setFilter, setFilterImmediately, debouncedModel],
    );
}

interface Props<M> {
    getValueLinker?: ValueLinker<M>;
}

export function FiltersContextProvider<M>({ children, getValueLinker }: PropsWithChildren<Props<M>>): JSX.Element {
    const [searchParams, _setSearchParams] = useSearchParams();
    const setSearchParams = useDebounce(_setSearchParams, 100);

    const [model = {}, setModel] = useState<M>(deserializeFromQuery(searchParams));

    useEffect(() => {
        setSearchParams(serializeToQuery(model), { replace: true });
    }, [model, setSearchParams]);

    const filtersModel = useMemo(() => ({ setModel, model }), [model]);

    return (
        <ValueLinkerContext.Provider value={getValueLinker}>
            <FiltersModelContext.Provider value={filtersModel}>{children}</FiltersModelContext.Provider>
        </ValueLinkerContext.Provider>
    );
}