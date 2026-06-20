"use client";

import { useState } from "react";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";

import { FileUploadSection } from "../components/FileUploadSection";
import { JobDetailsSection } from "../components/JobDetailsSection";
import { OrderOptionsSection } from "../components/OrderOptionsSection";
// import { CreateOrderActions } from "../components/CreateOrderActions";

import { CreateOrderForm } from "../types/create-order";

export function CreateOrderPage() {
  const [form, setForm] = useState<CreateOrderForm>({
    file: null,
    pages: null,
    copies: 1,
    paperSize: "A4",
    printType: "byn",
    hasCover: false,
    hasBinding: false,
    doubleSided: false,
    bound: false,
    spiralBound: false,
  });

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

        {/* <CreateOrderActions /> */}
      </div>
    </ClienteLayout>
  );
}