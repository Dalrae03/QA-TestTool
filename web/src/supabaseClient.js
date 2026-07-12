import { createClient } from "@supabase/supabase-js";

const url = import.meta.env.VITE_SUPABASE_URL;
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (!url || !anonKey) {
  console.warn(
    "[supabase] VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY 가 비어 있습니다. " +
    "web/.env.example 을 web/.env.local 로 복사해 값을 채우세요."
  );
}

export const supabase = createClient(url ?? "", anonKey ?? "");
export const isSupabaseConfigured = Boolean(url && anonKey);
