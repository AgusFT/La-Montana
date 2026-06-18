"use client";

import { LoginView } from "@/features/auth/LoginView";
import { DashboardView } from "@/features/dashboard/pages/DashboardPage";
import { useState } from "react";

export default function Home() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  if (isLoggedIn) {
    return <DashboardView onLogout={() => setIsLoggedIn(false)} />;
  }

  return <LoginView onLogin={() => setIsLoggedIn(true)} />;
}
