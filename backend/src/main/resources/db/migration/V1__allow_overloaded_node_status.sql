DO $$
BEGIN
    IF to_regclass('public.nodes') IS NOT NULL THEN
        ALTER TABLE public.nodes DROP CONSTRAINT IF EXISTS nodes_status_check;
        ALTER TABLE public.nodes
            ADD CONSTRAINT nodes_status_check
            CHECK (status IN ('ONLINE', 'OVERLOADED'));
    END IF;
END
$$;
