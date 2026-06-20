"use client";

// import { useState } from "react";

import {useRouter} from "next/navigation";
import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";

import { FileUploadSection } from "../components/crear_pedido/FileUploadSection";
import { JobDetailsSection } from "../components/crear_pedido/JobDetailsSection";
import { OrderOptionsSection } from "../components/crear_pedido/OrderOptionsSection";
import { CreateOrderActions } from "../components/crear_pedido/CreateOrderActions";

import { useCreateOrder } from "../context/CreateOrderContext";


export function CreateOrderPage() {
  const router = useRouter();

  const { form, setForm } =  useCreateOrder();

  const handleContinue = () => {
  router.push("/pedidos/resumen");
};

  return (
    <ClienteLayout>
      <div className="dashboard-main">
        <h1>Crear nuevo pedido</h1>

        <FileUploadSection
          file={form.file}
          onFileSelect={(file) =>
            setForm((prev) => ({
              ...prev,
              file,
            }))
          }
        />

        <div className="order-config-row">
          <JobDetailsSection
            form={form}
            onChange={(field, value) =>
              setForm((prev) => ({
              ...prev,
            [field]: value,
              }))
            }
          />

          <OrderOptionsSection 
            form={form}
            
            onChange={(field, value) =>
              setForm((prev) => ({
              ...prev,
            [field]: value,
              }))
            }
          />
        </div>

        <CreateOrderActions
            onSubmit={handleContinue}           
        />
      </div>
    </ClienteLayout>
  );
}