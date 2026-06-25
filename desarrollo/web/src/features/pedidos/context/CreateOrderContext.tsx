"use client";

import { createContext, useContext, useState } from "react";

import { CreateOrderForm } from "../types/create-order";

interface CreateOrderContextType {
  form: CreateOrderForm;
  setForm: React.Dispatch<
    React.SetStateAction<CreateOrderForm>
  >;
}

const CreateOrderContext = createContext<CreateOrderContextType | null>(
  null,
);

export function CreateOrderProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [form, setForm] = useState<CreateOrderForm>({
    file: null,
    pages: null,
    copies: 1,
    paperSize: "A4",
    printType: "byn",
    doubleSided: false,
    bound: false,
    spiralBound: false,
    paymentMethod: "Efectivo",
    deliveryPointId: null,
  });

  return (
    <CreateOrderContext.Provider
      value={{ form, setForm }}
    >
      {children}
    </CreateOrderContext.Provider>
  );
}

export function useCreateOrder() {
  const context = useContext(CreateOrderContext);

  if (!context) {
    throw new Error(
      "useCreateOrder debe usarse dentro de CreateOrderProvider",
    );
  }

  return context;
}
